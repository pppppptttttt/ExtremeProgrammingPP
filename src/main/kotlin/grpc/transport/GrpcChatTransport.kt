package grpc.transport

import domain.command.ConnectCommand
import domain.command.SendMessageCommand
import domain.command.StartServerCommand
import domain.model.ChatEvent
import domain.model.ChatMessage
import domain.model.PeerInfo
import domain.port.ChatTransport
import grpc.interceptor.GrpcPeerContext
import grpc.interceptor.PeerAddressInterceptor
import grpc.server.ChatGrpcServer
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hse.chat.v1.ChatServiceGrpc
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.hse.chat.v1.ChatMessage as ProtoChatMessage

/**
 * Адаптер [ChatTransport] поверх gRPC bidirectional streaming.
 */
class GrpcChatTransport : ChatTransport {
    private enum class Mode { IDLE, SERVER, CLIENT }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _events =
        MutableSharedFlow<ChatEvent>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val events: Flow<ChatEvent> = _events.asSharedFlow()

    private val lock = Any()
    private var mode = Mode.IDLE

    @Volatile
    private var outgoing: StreamObserver<ProtoChatMessage>? = null

    @Volatile
    private var currentPeer: PeerInfo? = null

    private var grpcServer: ChatGrpcServer? = null
    private var managedChannel: ManagedChannel? = null

    override suspend fun startServer(command: StartServerCommand) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                check(mode == Mode.IDLE) { "Transport already active" }
                mode = Mode.SERVER
            }
            val service = ChatTransportGrpcService(this@GrpcChatTransport)
            val server =
                ChatGrpcServer(
                    bindHost = "0.0.0.0",
                    port = command.port,
                    service = service,
                    interceptors = listOf(PeerAddressInterceptor()),
                )
            synchronized(lock) {
                grpcServer = server
            }
            server.start()
        }

    override suspend fun connect(command: ConnectCommand) =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                check(mode == Mode.IDLE) { "Transport already active" }
                mode = Mode.CLIENT
            }
            val peer = command.peer
            val channel =
                ManagedChannelBuilder
                    .forAddress(peer.host, peer.port)
                    .usePlaintext()
                    .build()
            synchronized(lock) {
                managedChannel = channel
            }
            val stub = ChatServiceGrpc.newStub(channel)
            val requestObserver =
                stub.chat(
                    object : StreamObserver<ProtoChatMessage> {
                        override fun onNext(value: ProtoChatMessage) {
                            scope.launch {
                                _events.emit(ChatEvent.MessageReceived(value.toDomain()))
                            }
                        }

                        override fun onError(t: Throwable) {
                            scope.launch {
                                _events.emit(ChatEvent.Error(t.message ?: "Connection error", t))
                                _events.emit(ChatEvent.Disconnected(peer))
                                cleanupAfterStreamEnd()
                            }
                        }

                        override fun onCompleted() {
                            scope.launch {
                                _events.emit(ChatEvent.Disconnected(peer))
                                cleanupAfterStreamEnd()
                            }
                        }
                    },
                )
            synchronized(lock) {
                outgoing = requestObserver
                currentPeer = peer
            }
            scope.launch {
                _events.emit(ChatEvent.Connected(peer))
            }
            Unit
        }

    override suspend fun send(command: SendMessageCommand): ChatMessage =
        withContext(Dispatchers.IO) {
            val out =
                synchronized(lock) { outgoing }
                    ?: throw IllegalStateException("Нет активного соединения (дождитесь подключения peer)")
            val instant = Instant.now()
            val proto = buildProtoMessage(command.sender, command.text, instant)
            out.onNext(proto)
            ChatMessage(
                sender = command.sender,
                text = command.text,
                sentAt = instant,
            )
        }

    override suspend fun disconnect() =
        withContext(Dispatchers.IO) {
            (scope.coroutineContext[Job] as? CompletableJob)?.cancelChildren()
            val peer: PeerInfo?
            synchronized(lock) {
                peer = currentPeer
                outgoing?.let { obs ->
                    try {
                        obs.onCompleted()
                    } catch (_: Exception) {
                    }
                }
                outgoing = null
                currentPeer = null
                grpcServer?.shutdown()
                grpcServer = null
                val ch = managedChannel
                managedChannel = null
                ch?.shutdown()
                ch?.awaitTermination(5, TimeUnit.SECONDS)
                mode = Mode.IDLE
            }
            peer?.let { p ->
                scope.launch {
                    _events.emit(ChatEvent.Disconnected(p))
                }
            }
            Unit
        }

    override fun close() {
        runBlocking {
            disconnect()
        }
        scope.cancel()
    }

    internal fun tryAttachServerOutgoing(responseObserver: StreamObserver<ProtoChatMessage>): Boolean {
        val peer = GrpcPeerContext.PEER_INFO.get() ?: serverConnectedPeerPlaceholder()
        synchronized(lock) {
            if (mode != Mode.SERVER || outgoing != null) {
                return false
            }
            outgoing = responseObserver
            currentPeer = peer
        }
        scope.launch {
            _events.emit(ChatEvent.Connected(peer))
        }
        return true
    }

    internal fun onServerReceivedFromPeer(proto: ProtoChatMessage) {
        scope.launch {
            _events.emit(ChatEvent.MessageReceived(proto.toDomain()))
        }
    }

    internal fun onServerStreamError(t: Throwable) {
        scope.launch {
            _events.emit(ChatEvent.Error(t.message ?: "Stream error", t))
            val peer = clearOutgoingAndPeerLocked()
            peer?.let { _events.emit(ChatEvent.Disconnected(it)) }
        }
    }

    internal fun onServerStreamCompletedFromClient() {
        scope.launch {
            val peer = clearOutgoingAndPeerLocked()
            peer?.let { _events.emit(ChatEvent.Disconnected(it)) }
        }
    }

    private fun clearOutgoingAndPeerLocked(): PeerInfo? {
        synchronized(lock) {
            outgoing = null
            val peer = currentPeer
            currentPeer = null
            return peer
        }
    }

    private fun cleanupAfterStreamEnd() {
        synchronized(lock) {
            outgoing = null
            currentPeer = null
            mode = Mode.IDLE
            try {
                managedChannel?.shutdownNow()
            } catch (_: Exception) {
            }
            managedChannel = null
        }
    }

    /** Fallback, если [PeerAddressInterceptor] не в цепочке или в [Context] нет адреса. */
    private fun serverConnectedPeerPlaceholder(): PeerInfo = PeerInfo("peer", 1)
}
