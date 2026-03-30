package domain.model

/** Сетевой адрес удалённого peer: хост и порт TCP. */
data class PeerInfo(
    val host: String,
    val port: Int,
) {
    init {
        require(host.isNotBlank()) { "Host must not be blank" }
        require(port in NetworkPorts.MIN_TCP_PORT..NetworkPorts.MAX_TCP_PORT) {
            "Port must be in range ${NetworkPorts.MIN_TCP_PORT}..${NetworkPorts.MAX_TCP_PORT}"
        }
    }

    override fun toString(): String = "$host:$port"
}
