package grpc.transport

import domain.command.ConnectCommand
import domain.command.SendMessageCommand
import domain.command.StartServerCommand
import domain.model.ChatEvent
import domain.model.PeerInfo
import java.net.ServerSocket
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals

class GrpcChatTransportIntegrationTest {

    @Test
    fun duplexMultipleMessages() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val server = GrpcChatTransport()
        val client = GrpcChatTransport()
        val receivedAtServer = mutableListOf<String>()
        val receivedAtClient = mutableListOf<String>()

        val serverJob = launch {
            server.events.collect { e ->
                if (e is ChatEvent.MessageReceived) {
                    receivedAtServer.add(e.message.text)
                }
            }
        }
        val clientJob = launch {
            client.events.collect { e ->
                if (e is ChatEvent.MessageReceived) {
                    receivedAtClient.add(e.message.text)
                }
            }
        }
        try {
            server.startServer(StartServerCommand("alice", port))
            client.connect(ConnectCommand("bob", PeerInfo("127.0.0.1", port)))
            repeat(3) { i ->
                client.send(SendMessageCommand("bob", "from-client-$i"))
            }
            repeat(3) { i ->
                server.send(SendMessageCommand("alice", "from-server-$i"))
            }
            delay(800)
            assertContentEquals(
                listOf("from-client-0", "from-client-1", "from-client-2"),
                receivedAtServer,
            )
            assertContentEquals(
                listOf("from-server-0", "from-server-1", "from-server-2"),
                receivedAtClient,
            )
        } finally {
            serverJob.cancel()
            clientJob.cancel()
            client.disconnect()
            server.disconnect()
            client.close()
            server.close()
        }
    }
}
