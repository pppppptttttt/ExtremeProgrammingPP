package domain.error

/** Ожидаемые ошибки домена и транспорта при работе с чатом. */
sealed class ChatError(
    message: String,
) : RuntimeException(message) {
    /** Некорректное или пустое имя пользователя. */
    class InvalidUserName : ChatError("Invalid user name")

    /** Некорректный адрес или порт peer. */
    class InvalidPeerAddress : ChatError("Invalid peer address")

    /** Пустой текст сообщения. */
    class EmptyMessage : ChatError("Message must not be empty")

    /** Операция требует активного соединения, а его нет. */
    class NotConnected : ChatError("Not connected to a peer")

    /** Ошибка сетевого слоя или gRPC с пояснением [reason]. */
    class TransportFailure(
        reason: String,
    ) : ChatError(reason)
}
