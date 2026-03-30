package domain.model

/** События чата, приходящие из транспорта: сообщения, соединение, ошибки. */
sealed interface ChatEvent {
    /** Входящее сообщение от собеседника. */
    data class MessageReceived(
        val message: ChatMessage,
    ) : ChatEvent

    /** Соединение с [peer] установлено. */
    data class Connected(
        val peer: PeerInfo,
    ) : ChatEvent

    /** Соединение с [peer] завершено. */
    data class Disconnected(
        val peer: PeerInfo,
    ) : ChatEvent

    /** Служебное сообщение для консоли. */
    data class System(
        val text: String,
    ) : ChatEvent

    /** Ошибка с текстом [text] и необязательной [cause]. */
    data class Error(
        val text: String,
        val cause: Throwable? = null,
    ) : ChatEvent
}
