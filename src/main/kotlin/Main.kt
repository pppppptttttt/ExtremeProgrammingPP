import cli.ChatCliApp
import cli.CommandLineParser
import domain.port.ChatTransport
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) =
    runBlocking {
        val parsedArgs =
            try {
                CommandLineParser.parse(args)
            } catch (e: IllegalArgumentException) {
                System.err.println(e.message)
                return@runBlocking
            }

        val transport = createTransport()
        ChatCliApp(transport).run(parsedArgs)
    }

/**
 * Здесь должен появиться адаптер транспортного слоя.
 * Его реализует часть проекта с gRPC.
 */
private fun createTransport(): ChatTransport {
    error("ChatTransport implementation is not wired yet")
}
