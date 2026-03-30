package cli

import domain.model.ChatEvent
import domain.model.ChatMessage
import domain.model.PeerInfo
import java.io.PrintStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ConsoleRenderer(
    zoneId: ZoneId = ZoneId.systemDefault(),
    private val out: PrintStream = System.out,
    private val err: PrintStream = System.err,
) {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX").withZone(zoneId)

    @Synchronized
    fun printWelcome(args: AppArgs) {
        out.println("P2P chat started")
        out.println("user: ${args.selfName}")

        when (val mode = args.mode) {
            is LaunchMode.Server ->
                out.println("mode: server, listen on port ${mode.listenPort}")

            is LaunchMode.Client ->
                out.println("mode: client, connect to ${formatPeer(mode.peer)}")
        }

        out.println("commands: /help, /exit")
        printPrompt()
    }

    @Synchronized
    fun printHelp() {
        out.println("/help - показать команды")
        out.println("/exit - выйти из чата")
        printPrompt()
    }

    @Synchronized
    fun printOwnMessage(message: ChatMessage) {
        printMessageLine(message)
        printPrompt()
    }

    @Synchronized
    fun printEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.MessageReceived -> {
                printMessageLine(event.message)
            }

            is ChatEvent.Connected -> {
                out.println("[system] connected: ${formatPeer(event.peer)}")
            }

            is ChatEvent.Disconnected -> {
                out.println("[system] disconnected: ${formatPeer(event.peer)}")
            }

            is ChatEvent.System -> {
                out.println("[system] ${event.text}")
            }

            is ChatEvent.Error -> {
                err.println("[error] ${event.text}")
                event.cause?.message?.let { err.println("[error] cause: $it") }
            }
        }

        printPrompt()
    }

    @Synchronized
    fun printError(message: String) {
        err.println("[error] $message")
        printPrompt()
    }

    @Synchronized
    fun printError(error: Throwable) {
        err.println("[error] ${error.message ?: error::class.simpleName.orEmpty()}")
        printPrompt()
    }

    @Synchronized
    fun printPrompt() {
        out.print("> ")
        out.flush()
    }

    private fun printMessageLine(message: ChatMessage) {
        val timestamp = formatter.format(message.sentAt)
        out.println("[$timestamp] ${message.sender}: ${message.text}")
    }

    private fun formatPeer(peer: PeerInfo): String = "${peer.host}:${peer.port}"
}
