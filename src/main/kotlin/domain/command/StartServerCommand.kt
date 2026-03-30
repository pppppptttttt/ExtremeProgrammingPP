package domain.command

import domain.model.NetworkPorts

/** Команда запуска в режиме ожидания входящего подключения на [port]. */
data class StartServerCommand(
    val selfName: String,
    val port: Int,
) {
    init {
        require(port in NetworkPorts.MIN_TCP_PORT..NetworkPorts.MAX_TCP_PORT) {
            "Port must be in range ${NetworkPorts.MIN_TCP_PORT}..${NetworkPorts.MAX_TCP_PORT}"
        }
    }
}
