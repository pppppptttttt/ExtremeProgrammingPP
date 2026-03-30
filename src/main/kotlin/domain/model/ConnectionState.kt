package domain.model

sealed interface ConnectionState {
    data object Idle : ConnectionState

    data object Listening : ConnectionState

    data class Connecting(
        val peer: PeerInfo,
    ) : ConnectionState

    data class Connected(
        val peer: PeerInfo,
    ) : ConnectionState

    data class Failed(
        val reason: String,
    ) : ConnectionState
}
