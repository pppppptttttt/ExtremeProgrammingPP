package domain.command

/** Команда отправки исходящего сообщения от [sender]. */
data class SendMessageCommand(
    val sender: String,
    val text: String,
) {
    init {
        require(text.isNotBlank()) { "Message text must not be blank" }
    }
}
