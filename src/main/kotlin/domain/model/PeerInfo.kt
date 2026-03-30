package domain.model

data class PeerInfo(
    val host: String,
    val port: Int
) {
    init {
        require(host.isNotBlank()) { "Host must not be blank" }
        require(port in 1..65535) { "Port must be in range 1..65535" }
    }

    override fun toString(): String = "$host:$port"
}
