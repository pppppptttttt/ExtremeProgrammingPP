package grpc.transport

import domain.command.ConnectCommand
import domain.command.StartServerCommand
import domain.model.ChatEvent
import domain.model.PeerInfo
import io.grpc.Status
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class GrpcChatTransportNegativeTest {

    @Test
    fun secondClientRejectedWhenServerAlreadyHasPeer() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val server = GrpcChatTransport()
        val client1 = GrpcChatTransport()
        val client2 = GrpcChatTransport()
        try {
            server.startServer(StartServerCommand("alice", port))
            launch(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    server.events.first { it is ChatEvent.Connected }
                }
            }.join()
            client1.connect(ConnectCommand("bob", PeerInfo("127.0.0.1", port)))
            launch(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    server.events.first { it is ChatEvent.Connected }
                }
            }.join()

            val err = coroutineScope {
                val deferred = async {
                    client2.events.first { it is ChatEvent.Error } as ChatEvent.Error
                }
                client2.connect(ConnectCommand("charlie", PeerInfo("127.0.0.1", port)))
                withTimeout(15_000) { deferred.await() }
            }
            assertNotNull(err.cause)
            val code = Status.fromThrowable(err.cause).code
            assertEquals(Status.RESOURCE_EXHAUSTED.code, code)
        } finally {
            runCatching { client1.disconnect() }
            runCatching { client2.disconnect() }
            runCatching { server.disconnect() }
            client1.close()
            client2.close()
            server.close()
        }
    }

    @Test
    fun serverSeesDisconnectedWhenClientDisconnects() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val server = GrpcChatTransport()
        val client = GrpcChatTransport()
        try {
            server.startServer(StartServerCommand("alice", port))
            launch(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    server.events.first { it is ChatEvent.Connected }
                }
            }.join()
            client.connect(ConnectCommand("bob", PeerInfo("127.0.0.1", port)))
            launch(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(10_000) {
                    server.events.first { it is ChatEvent.Connected }
                }
            }.join()

            val disconnected = coroutineScope {
                val disc = async {
                    server.events.first { it is ChatEvent.Disconnected }
                }
                client.disconnect()
                withTimeout(10_000) { disc.await() }
            }
            assertIs<ChatEvent.Disconnected>(disconnected)
        } finally {
            runCatching { client.disconnect() }
            runCatching { server.disconnect() }
            client.close()
            server.close()
        }
    }
}
