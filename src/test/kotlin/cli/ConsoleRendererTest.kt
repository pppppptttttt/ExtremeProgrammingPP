package cli

import domain.model.ChatEvent
import domain.model.ChatMessage
import domain.model.PeerInfo
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertTrue

class ConsoleRendererTest {
    @Test
    fun `prints own message with sender timestamp and text`() {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val renderer = newRenderer(out, err)

        renderer.printOwnMessage(
            ChatMessage(
                sender = "Alice",
                text = "hello",
                sentAt = Instant.parse("2026-03-30T12:00:00Z"),
                id = "msg-1"
            )
        )

        val printed = out.utf8()
        assertTrue(printed.contains("[2026-03-30 12:00:00 Z] Alice: hello"))
        assertTrue(printed.contains("> "))
    }

    @Test
    fun `prints incoming message event`() {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val renderer = newRenderer(out, err)

        renderer.printEvent(
            ChatEvent.MessageReceived(
                ChatMessage(
                    sender = "Bob",
                    text = "hi",
                    sentAt = Instant.parse("2026-03-30T13:15:00Z"),
                    id = "msg-2"
                )
            )
        )

        val printed = out.utf8()
        assertTrue(printed.contains("[2026-03-30 13:15:00 Z] Bob: hi"))
    }

    @Test
    fun `prints connection event`() {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val renderer = newRenderer(out, err)

        renderer.printEvent(ChatEvent.Connected(PeerInfo("localhost", 50051)))

        assertTrue(out.utf8().contains("[system] connected: localhost:50051"))
    }

    @Test
    fun `prints error event to stderr`() {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val renderer = newRenderer(out, err)

        renderer.printEvent(
            ChatEvent.Error(
                text = "transport failed",
                cause = IllegalStateException("boom")
            )
        )

        val printedErr = err.utf8()
        assertTrue(printedErr.contains("[error] transport failed"))
        assertTrue(printedErr.contains("[error] cause: boom"))
    }

    @Test
    fun `prints welcome for server mode`() {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val renderer = newRenderer(out, err)

        renderer.printWelcome(
            AppArgs(
                selfName = "Alice",
                mode = LaunchMode.Server(50051)
            )
        )

        val printed = out.utf8()
        assertTrue(printed.contains("P2P chat started"))
        assertTrue(printed.contains("user: Alice"))
        assertTrue(printed.contains("mode: server, listen on port 50051"))
        assertTrue(printed.contains("commands: /help, /exit"))
    }

    private fun newRenderer(
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
}
