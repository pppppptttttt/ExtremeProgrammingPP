package grpc.client

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.hse.chat.v1.ChatServiceGrpc
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Исходящее подключение: [ManagedChannel] и async stub [ChatServiceGrpc.ChatServiceStub].
 */
class ChatGrpcClient(
    private val host: String,
    private val port: Int,
) {
    private val logger = Logger.getLogger(ChatGrpcClient::class.java.name)

    private var channel: ManagedChannel? = null

    fun connect() {
        check(channel == null) { "Channel already open" }
        val built =
            ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build()
        channel = built
        logger.info("gRPC channel created: $host:$port (plaintext)")
    }

    fun asyncStub(): ChatServiceGrpc.ChatServiceStub {
        val ch = channel ?: error("Not connected; call connect() first")
        return ChatServiceGrpc.newStub(ch)
    }

    fun shutdown() {
        val ch = channel ?: return
        logger.info("Shutting down gRPC channel to $host:$port")
        ch.shutdown()
        try {
            if (!ch.awaitTermination(5, TimeUnit.SECONDS)) {
                ch.shutdownNow()
                if (!ch.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Channel did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            ch.shutdownNow()
            Thread.currentThread().interrupt()
        } finally {
            channel = null
        }
    }
}
