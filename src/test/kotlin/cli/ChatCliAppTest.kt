package cli

import domain.command.ConnectCommand
import domain.command.SendMessageCommand
import domain.command.StartServerCommand
import domain.model.ChatEvent
import domain.model.ChatMessage
import domain.model.PeerInfo
import domain.port.ChatTransport
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatCliAppTest {
    @Test
    fun `server mode starts server and disconnects on exit`() = runBlocking {
        val transport = FakeChatTransport()
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val app = ChatCliApp(
            transport = transport,
            renderer = renderer(out, err),
            readLineFn = StubLineReader("/exit")::next,
        )

        app.run(
            AppArgs(
                selfName = "Alice",
                mode = LaunchMode.Server(50051)
            )
        )

        assertEquals(
            StartServerCommand(selfName = "Alice", port = 50051),
            transport.startedCommand
        )
        assertTrue(transport.disconnected)
        assertTrue(out.utf8().contains("mode: server, listen on port 50051"))
    }

    @Test
    fun `client mode connects sends message and prints sent message`() = runBlocking {
        val transport = FakeChatTransport()
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val app = ChatCliApp(
            transport = transport,
            renderer = renderer(out, err),
            readLineFn = StubLineReader("hello", "/exit")::next,
        )

        app.run(
            AppArgs(
                selfName = "Bob",
                mode = LaunchMode.Client(PeerInfo("127.0.0.1", 50051))
            )
        )

        assertEquals(
            ConnectCommand(
                selfName = "Bob",
                peer = PeerInfo("127.0.0.1", 50051)
            ),
            transport.connectedCommand
        )
        assertEquals(
            listOf(SendMessageCommand(sender = "Bob", text = "hello")),
            transport.sentCommands
        )
        assertTrue(out.utf8().contains("[2026-03-30 12:00:00 Z] Bob: hello"))
        assertTrue(transport.disconnected)
    }

    @Test
    fun `help command does not send anything`() = runBlocking {
        val transport = FakeChatTransport()
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val app = ChatCliApp(
            transport = transport,
            renderer = renderer(out, err),
            readLineFn = StubLineReader("/help", "/exit")::next,
        )

        app.run(
            AppArgs(
                selfName = "Alice",
                mode = LaunchMode.Server(50051)
            )
        )

        assertTrue(transport.sentCommands.isEmpty())
        assertTrue(out.utf8().contains("/help - показать команды"))
        assertTrue(out.utf8().contains("/exit - выйти из чата"))
    }

    @Test
    fun `blank input is ignored`() = runBlocking {
        val transport = FakeChatTransport()
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()

        val app = ChatCliApp(
            transport = transport,
            renderer = renderer(out, err),
            readLineFn = StubLineReader("   ", "/exit")::next,
        )

        app.run(
            AppArgs(
                selfName = "Alice",
                mode = LaunchMode.Server(50051)
            )
        )

        assertTrue(transport.sentCommands.isEmpty())
    }

    private fun renderer(
        out: ByteArrayOutputStream,
        err: ByteArrayOutputStream,
    ): ConsoleRenderer {
        return ConsoleRenderer(
            zoneId = ZoneId.of("UTC"),
            out = PrintStream(out, true, StandardCharsets.UTF_8),
            err = PrintStream(err, true, StandardCharsets.UTF_8),
        )
    }

    private fun ByteArrayOutputStream.utf8(): String =
        String(this.toByteArray(), StandardCharsets.UTF_8)

    private class StubLineReader(vararg lines: String) {
        private val iterator = lines.iterator()

        fun next(): String? = if (iterator.hasNext()) iterator.next() else null
    }

    private class FakeChatTransport : ChatTransport {
        private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
        override val events: Flow<ChatEvent> = _events

        var startedCommand: StartServerCommand? = null
        var connectedCommand: ConnectCommand? = null
        val sentCommands: MutableList<SendMessageCommand> = mutableListOf()
        var disconnected: Boolean = false

        override suspend fun startServer(command: StartServerCommand) {
            startedCommand = command
        }

        override suspend fun connect(command: ConnectCommand) {
            connectedCommand = command
        }

        override suspend fun send(command: SendMessageCommand): ChatMessage {
            sentCommands += command
            return ChatMessage(
                sender = command.sender,
                text = command.text,
                sentAt = Instant.parse("2026-03-30T12:00:00Z"),
                id = "test-message-id"
            )
        }

        override suspend fun disconnect() {
            disconnected = true
        }

        override fun close() = Unit
    }
}
