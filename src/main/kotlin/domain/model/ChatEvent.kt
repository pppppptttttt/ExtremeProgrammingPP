package domain.model

sealed interface ChatEvent {
    data class MessageReceived(
        val message: ChatMessage,
    ) : ChatEvent

    data class Connected(
        val peer: PeerInfo
    ) : ChatEvent

    data class Disconnected(
        val peer: PeerInfo
    ) : ChatEvent

    data class System(
        val text: String
    ) : ChatEvent

    data class Error(
        val text: String,
        val cause: Throwable? = null
    ) : ChatEvent
}
