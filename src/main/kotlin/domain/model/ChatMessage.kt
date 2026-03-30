package domain.model

import java.time.Instant
import java.util.UUID

data class ChatMessage(
    val sender: String,
    val text: String,
    val sentAt: Instant,
    val id: String = UUID.randomUUID().toString(),
) {
    init {
        require(text.isNotBlank()) { "Message text must not be blank" }
    }
}
