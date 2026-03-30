package cli

import domain.command.ConnectCommand
import domain.command.SendMessageCommand
import domain.command.StartServerCommand
import domain.port.ChatTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Консольное приложение чата: подписка на [ChatTransport.events], запуск сервера или клиента,
 * цикл ввода и отправка [SendMessageCommand].
 */
class ChatCliApp(
    private val transport: ChatTransport,
    private val renderer: ConsoleRenderer = ConsoleRenderer(),
    private val readLineFn: () -> String? = ::readLine,
) {
    /** Запуск сценария по [args]: транспорт, приветствие, чтение stdin до `/exit` или EOF. */
    suspend fun run(args: AppArgs) =
        coroutineScope {
            val eventJob =
                launch {
                    transport.events.collect { event ->
                        renderer.printEvent(event)
                    }
                }

            try {
                when (val mode = args.mode) {
                    is LaunchMode.Server -> {
                        transport.startServer(
                            StartServerCommand(
                                selfName = args.selfName,
                                port = mode.listenPort,
                            ),
                        )
                    }

                    is LaunchMode.Client -> {
                        transport.connect(
                            ConnectCommand(
                                selfName = args.selfName,
                                peer = mode.peer,
                            ),
                        )
                    }
                }

                renderer.printWelcome(args)
                inputLoop(args.selfName)
            } finally {
                try {
                    transport.disconnect()
                } finally {
                    eventJob.cancel()
                    transport.close()
                }
            }
        }

    private suspend fun inputLoop(selfName: String) =
        withContext(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                val line = readLineFn() ?: break
                val trimmed = line.trim()

                if (trimmed.isEmpty()) {
                    renderer.printPrompt()
                    continue
                }

                when (trimmed) {
                    "/help" -> renderer.printHelp()
                    "/exit" -> return@withContext
                    else -> {
                        try {
                            val sentMessage =
                                transport.send(
                                    SendMessageCommand(
                                        sender = selfName,
                                        text = trimmed,
                                    ),
                                )
                            renderer.printOwnMessage(sentMessage)
                        } catch (t: Throwable) {
                            renderer.printError(t)
                        }
                    }
                }
            }
        }
}
