package domain.command

import domain.model.PeerInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommandTest {
    @Test
    fun `creates connect command`() {
        val peer = PeerInfo("localhost", 50051)
        val command = ConnectCommand(selfName = "Alice", peer = peer)

        assertEquals("Alice", command.selfName)
        assertEquals(peer, command.peer)
    }

    @Test
    fun `send message rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            SendMessageCommand(sender = "Alice", text = "   ")
        }
    }

    @Test
    fun `start server rejects invalid port`() {
        assertFailsWith<IllegalArgumentException> {
            StartServerCommand(selfName = "Alice", port = 0)
        }

        assertFailsWith<IllegalArgumentException> {
            StartServerCommand(selfName = "Alice", port = 65536)
        }
    }

    @Test
    fun `creates valid start server command`() {
        val command = StartServerCommand(selfName = "Alice", port = 50051)

        assertEquals("Alice", command.selfName)
        assertEquals(50051, command.port)
    }
}
