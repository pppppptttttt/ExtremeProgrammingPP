package cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CommandLineParserTest {
    @Test
    fun `parses server mode`() {
        val args = arrayOf(
            "--name", "Alice",
            "--listen-port", "50051"
        )

        val parsed = CommandLineParser.parse(args)

        assertEquals("Alice", parsed.selfName)
        val mode = assertIs<LaunchMode.Server>(parsed.mode)
        assertEquals(50051, mode.listenPort)
    }

    @Test
    fun `parses client mode`() {
        val args = arrayOf(
            "--name", "Bob",
            "--peer-host", "127.0.0.1",
            "--peer-port", "50051"
        )

        val parsed = CommandLineParser.parse(args)

        assertEquals("Bob", parsed.selfName)
        val mode = assertIs<LaunchMode.Client>(parsed.mode)
        assertEquals("127.0.0.1", mode.peer.host)
        assertEquals(50051, mode.peer.port)
    }

    @Test
    fun `fails when mode is missing`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            CommandLineParser.parse(arrayOf("--name", "Alice"))
        }

        assert(ex.message!!.contains("Нужно указать либо --listen-port"))
    }

    @Test
    fun `fails when peer host and peer port are incomplete`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            CommandLineParser.parse(
                arrayOf(
                    "--name", "Bob",
                    "--peer-host", "127.0.0.1"
                )
            )
        }

        assert(ex.message!!.contains("--peer-host и --peer-port"))
    }

    @Test
    fun `fails on invalid port`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            CommandLineParser.parse(
                arrayOf(
                    "--name", "Alice",
                    "--listen-port", "abc"
                )
            )
        }

        assert(ex.message!!.contains("--listen-port должен быть числом"))
    }

    @Test
    fun `help prints usage through exception`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            CommandLineParser.parse(arrayOf("--help"))
        }

        assert(ex.message!!.contains("Использование:"))
    }
}
