package grpc.client

import grpc.server.ChatGrpcServer
import kotlin.test.Test
import kotlin.test.assertEquals

class EchoGrpcIntegrationTest {
    @Test
    fun echoRoundTrip() {
        val server = ChatGrpcServer("127.0.0.1", 0)
        server.start()
        val port = server.boundPort()
        try {
            val client = ChatGrpcClient("127.0.0.1", port)
            client.connect()
            try {
                val got = echoProbe(client, "test", "hello")
                assertEquals("hello", got.text)
                assertEquals("test", got.sender)
            } finally {
                client.shutdown()
            }
        } finally {
            server.shutdown()
        }
    }
}
