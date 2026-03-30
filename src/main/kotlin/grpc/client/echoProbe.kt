package grpc.client

import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import org.hse.chat.v1.ChatMessage
import org.hse.chat.v1.ChatServiceGrpc
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

private val echoProbeLogger = Logger.getLogger("grpc.client.echoProbe")

/**
 * Одно исходящее сообщение в bidirectional stream и одно входящее (эхо сервера).
 * Общий сценарий для ручного `--client` и для интеграционных тестов.
 */
fun echoProbe(client: ChatGrpcClient, sender: String, text: String): ChatMessage {
    val stub = client.asyncStub()
    val latch = CountDownLatch(1)
    var streamError: Throwable? = null
    var received: ChatMessage? = null

    val requestObserver = stub.chat(object : StreamObserver<ChatMessage> {
        override fun onNext(value: ChatMessage) {
            received = value
            echoProbeLogger.info("Received from peer: sender=${value.sender} text=${value.text}")
            latch.countDown()
        }

        override fun onError(t: Throwable) {
            echoProbeLogger.log(Level.WARNING, "Chat stream error", t)
            streamError = t
            latch.countDown()
        }

        override fun onCompleted() {
            echoProbeLogger.fine("Server closed response stream")
        }
    })

    val now = Instant.now()
    val ts = Timestamp.newBuilder()
        .setSeconds(now.epochSecond)
        .setNanos(now.nano)
        .build()

    val msg = ChatMessage.newBuilder()
        .setSender(sender)
        .setText(text)
        .setSentAt(ts)
        .build()

    requestObserver.onNext(msg)
    requestObserver.onCompleted()

    if (!latch.await(15, TimeUnit.SECONDS)) {
        error("Echo probe timed out waiting for echo")
    }
    streamError?.let { throw it }
    return received ?: error("No echo received")
}
