package domain.error

sealed class ChatError(
    message: String,
) : RuntimeException(message) {
    class InvalidUserName : ChatError("Invalid user name")

    class InvalidPeerAddress : ChatError("Invalid peer address")

    class EmptyMessage : ChatError("Message must not be empty")

    class NotConnected : ChatError("Not connected to a peer")

    class TransportFailure(
        reason: String,
    ) : ChatError(reason)
}
