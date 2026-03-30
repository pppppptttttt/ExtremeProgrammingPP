package domain.command

/** Команда запуска в режиме ожидания входящего подключения на [port]. */
data class StartServerCommand(
    val selfName: String,
    val port: Int,
) {
    init {
        require(port in 1..65535) { "Port must be in range 1..65535" }
    }
}
