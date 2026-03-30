package grpc.transport

import com.google.protobuf.Timestamp
import domain.model.ChatMessage
import java.time.Instant
import java.util.UUID
import org.hse.chat.v1.ChatMessage as ProtoChatMessage

internal fun Instant.toProtoTimestamp(): Timestamp =
    Timestamp.newBuilder()
        .setSeconds(epochSecond)
        .setNanos(nano)
        .build()

internal fun Timestamp.toInstantOrNow(): Instant =
    if (seconds == 0L && nanos == 0) {
        Instant.now()
    } else {
        Instant.ofEpochSecond(seconds, nanos.toLong())
    }

internal fun ProtoChatMessage.toDomain(): ChatMessage =
    ChatMessage(
        sender = sender,
        text = text,
        sentAt = if (hasSentAt()) sentAt.toInstantOrNow() else Instant.now(),
        id = UUID.randomUUID().toString(),
    )

internal fun buildProtoMessage(sender: String, text: String, sentAt: Instant): ProtoChatMessage =
    ProtoChatMessage.newBuilder()
        .setSender(sender)
        .setText(text)
        .setSentAt(sentAt.toProtoTimestamp())
        .build()
