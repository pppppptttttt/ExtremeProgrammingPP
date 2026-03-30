package cli

import domain.model.ChatEvent
import domain.model.ChatMessage
import domain.model.PeerInfo
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ConsoleRenderer(
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX").withZone(zoneId)

    @Synchronized
    fun printWelcome(args: AppArgs) {
        println("P2P chat started")
        println("user: ${args.selfName}")

        when (val mode = args.mode) {
            is LaunchMode.Server ->
                println("mode: server, listen on port ${mode.listenPort}")

            is LaunchMode.Client ->
                println("mode: client, connect to ${formatPeer(mode.peer)}")
        }

        println("commands: /help, /exit")
        printPrompt()
    }

    @Synchronized
    fun printHelp() {
        println("/help - показать команды")
        println("/exit - выйти из чата")
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
                println("[system] connected: ${formatPeer(event.peer)}")
            }

            is ChatEvent.Disconnected -> {
                println("[system] disconnected: ${formatPeer(event.peer)}")
            }

            is ChatEvent.System -> {
                println("[system] ${event.text}")
            }

            is ChatEvent.Error -> {
                System.err.println("[error] ${event.text}")
                event.cause?.message?.let { System.err.println("[error] cause: $it") }
            }
        }

        printPrompt()
    }

    @Synchronized
    fun printError(message: String) {
        System.err.println("[error] $message")
        printPrompt()
    }

    @Synchronized
    fun printError(error: Throwable) {
        System.err.println("[error] ${error.message ?: error::class.simpleName.orEmpty()}")
        printPrompt()
    }

    @Synchronized
    fun printPrompt() {
        print("> ")
        System.out.flush()
    }

    private fun printMessageLine(message: ChatMessage) {
        val timestamp = formatter.format(message.sentAt)
        println("[$timestamp] ${message.sender}: ${message.text}")
    }

    private fun formatPeer(peer: PeerInfo): String = "${peer.host}:${peer.port}"
}
