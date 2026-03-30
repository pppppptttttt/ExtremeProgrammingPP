import cli.ChatCliApp
import cli.CommandLineParser
import domain.port.ChatTransport
import grpc.transport.GrpcChatTransport
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val parsedArgs = try {
        CommandLineParser.parse(args)
    } catch (e: IllegalArgumentException) {
        System.err.println(e.message)
        return@runBlocking
    }

    val transport = createTransport()
    ChatCliApp(transport).run(parsedArgs)
}

/** gRPC-реализация [ChatTransport] — см. [GrpcChatTransport]. */
private fun createTransport(): ChatTransport = GrpcChatTransport()
