package domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeerInfoTest {
    @Test
    fun `creates peer info`() {
        val peer = PeerInfo(host = "127.0.0.1", port = 50051)

        assertEquals("127.0.0.1", peer.host)
        assertEquals(50051, peer.port)
    }

    @Test
    fun `rejects blank host`() {
        assertFailsWith<IllegalArgumentException> {
            PeerInfo(host = "   ", port = 50051)
        }
    }

    @Test
    fun `rejects invalid port`() {
        assertFailsWith<IllegalArgumentException> {
            PeerInfo(host = "localhost", port = 0)
        }

        assertFailsWith<IllegalArgumentException> {
            PeerInfo(host = "localhost", port = 70000)
        }
    }

    @Test
    fun `formats as host and port`() {
        assertEquals("localhost:8080", PeerInfo("localhost", 8080).toString())
    }
}
