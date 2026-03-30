package cli

import domain.model.PeerInfo

data class AppArgs(
    val selfName: String,
    val mode: LaunchMode,
)

sealed interface LaunchMode {
    data class Server(
        val listenPort: Int,
    ) : LaunchMode

    data class Client(
        val peer: PeerInfo,
    ) : LaunchMode
}
