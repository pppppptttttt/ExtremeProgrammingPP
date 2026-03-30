package domain.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChatMessageTest {
    @Test
    fun `stores explicit sentAt`() {
        val sentAt = Instant.parse("2026-03-30T12:34:56Z")

        val message = ChatMessage(
            sender = "Alice",
            text = "hello",
            sentAt = sentAt,
            id = "msg-1"
        )

        assertEquals("Alice", message.sender)
        assertEquals("hello", message.text)
        assertEquals(sentAt, message.sentAt)
        assertEquals("msg-1", message.id)
    }

    @Test
    fun `rejects blank text`() {
        assertFailsWith<IllegalArgumentException> {
            ChatMessage(
                sender = "Alice",
                text = "   ",
                sentAt = Instant.parse("2026-03-30T12:34:56Z")
            )
        }
    }
}
