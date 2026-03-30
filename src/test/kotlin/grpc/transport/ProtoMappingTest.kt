package grpc.transport

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtoMappingTest {

    @Test
    fun roundTripInstant() {
        val instant = Instant.parse("2020-01-02T12:34:56.123Z")
        val proto = buildProtoMessage("u", "hi", instant)
        val domain = proto.toDomain()
        assertEquals("u", domain.sender)
        assertEquals("hi", domain.text)
        assertEquals(instant.epochSecond, domain.sentAt.epochSecond)
        assertEquals(instant.nano, domain.sentAt.nano)
    }
}
