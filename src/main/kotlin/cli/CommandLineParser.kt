package cli

import domain.model.PeerInfo

object CommandLineParser {
    fun parse(args: Array<String>): AppArgs {
        if (args.isEmpty() || args.contains("--help")) {
            throw IllegalArgumentException(usage())
        }

        val options = parseOptions(args)

        val selfName =
            options["--name"]?.trim()
                ?: throw IllegalArgumentException("Не указан --name\n\n${usage()}")

        require(selfName.isNotBlank()) { "Имя пользователя не должно быть пустым" }

        val listenPort = options["--listen-port"]
        val peerHost = options["--peer-host"]
        val peerPort = options["--peer-port"]

        return when {
            peerHost != null || peerPort != null -> {
                if (peerHost == null || peerPort == null) {
                    throw IllegalArgumentException(
                        "Для режима клиента нужны оба аргумента: --peer-host и --peer-port\n\n${usage()}",
                    )
                }

                AppArgs(
                    selfName = selfName,
                    mode =
                        LaunchMode.Client(
                            peer =
                                PeerInfo(
                                    host = peerHost,
                                    port = parsePort(peerPort, "--peer-port"),
                                ),
                        ),
                )
            }

            listenPort != null -> {
                AppArgs(
                    selfName = selfName,
                    mode =
                        LaunchMode.Server(
                            listenPort = parsePort(listenPort, "--listen-port"),
                        ),
                )
            }

            else -> {
                throw IllegalArgumentException(
                    "Нужно указать либо --listen-port, либо пару --peer-host/--peer-port\n\n${usage()}",
                )
            }
        }
    }

    private fun parseOptions(args: Array<String>): Map<String, String> {
        require(args.size % 2 == 0) {
            "Аргументы должны идти парами ключ-значение\n\n${usage()}"
        }

        val result = linkedMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val key = args[i]
            val value =
                args.getOrNull(i + 1)
                    ?: throw IllegalArgumentException("Для аргумента $key не передано значение")

            require(key.startsWith("--")) { "Неожиданный аргумент: $key" }
            result[key] = value
            i += 2
        }
        return result
    }

    private fun parsePort(
        value: String,
        argName: String,
    ): Int {
        val port =
            value.toIntOrNull()
                ?: throw IllegalArgumentException("$argName должен быть числом")

        require(port in 1..65535) {
            "$argName должен быть в диапазоне 1..65535"
        }

        return port
    }

    fun usage(): String =
        """
        Использование:
          Сервер:
            --name <username> --listen-port <port>

          Клиент:
            --name <username> --peer-host <host> --peer-port <port>

        Примеры:
          --name Alice --listen-port 50051
          --name Bob --peer-host 127.0.0.1 --peer-port 50051
        """.trimIndent()
}
