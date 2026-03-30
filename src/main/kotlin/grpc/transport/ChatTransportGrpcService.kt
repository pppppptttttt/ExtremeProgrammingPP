package grpc.transport

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.hse.chat.v1.ChatMessage
import org.hse.chat.v1.ChatServiceGrpc

/**
 * Реализация [ChatService] для P2P: один активный bidirectional stream на процесс.
 */
internal class ChatTransportGrpcService(
    private val transport: GrpcChatTransport,
) : ChatServiceGrpc.ChatServiceImplBase() {
    override fun chat(responseObserver: StreamObserver<ChatMessage>): StreamObserver<ChatMessage> {
        val accepted = transport.tryAttachServerOutgoing(responseObserver)
        if (!accepted) {
            responseObserver.onError(
                Status.RESOURCE_EXHAUSTED.withDescription("Only one peer at a time").asRuntimeException(),
            )
            return object : StreamObserver<ChatMessage> {
                override fun onNext(value: ChatMessage) = Unit

                override fun onError(t: Throwable) = Unit

                override fun onCompleted() = Unit
            }
        }
        return object : StreamObserver<ChatMessage> {
            override fun onNext(value: ChatMessage) {
                transport.onServerReceivedFromPeer(value)
            }

            override fun onError(t: Throwable) {
                transport.onServerStreamError(t)
            }

            override fun onCompleted() {
                transport.onServerStreamCompletedFromClient()
            }
        }
    }
}
