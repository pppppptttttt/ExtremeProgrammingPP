package domain.command

data class SendMessageCommand(
    val sender: String, // username
    val text: String,
) {
    init {
        require(text.isNotBlank()) { "Message text must not be blank" }
    }
}
