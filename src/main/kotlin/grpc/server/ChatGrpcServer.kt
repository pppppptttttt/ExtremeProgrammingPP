package grpc.server

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerInterceptor
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Поднимает gRPC [Server] с переданным сервисом (по умолчанию — [EchoChatService] для тестов/дыма).
 */
class ChatGrpcServer(
    private val bindHost: String,
    private val port: Int,
    private val service: BindableService = EchoChatService(),
    private val interceptors: List<ServerInterceptor> = emptyList(),
) {
    private val logger = Logger.getLogger(ChatGrpcServer::class.java.name)

    private var server: Server? = null

    fun start() {
        check(server == null) { "Server already started" }
        val socket = InetSocketAddress(bindHost, port)
        val built =
            NettyServerBuilder
                .forAddress(socket)
                .apply {
                    interceptors.forEach { intercept(it) }
                }.addService(service)
                .build()
        built.start()
        server = built
        logger.info("gRPC server listening on $bindHost:${boundPort()}")
    }

    /** Фактический порт после [start] (если в конструкторе был `0` — выбран ОС). */
    fun boundPort(): Int = checkNotNull(server) { "Server not started" }.port

    /** Блокирует текущий поток до [Server.shutdown] (например из shutdown-hook). */
    fun blockUntilShutdown() {
        val s = server ?: error("Server not started")
        s.awaitTermination()
    }

    /**
     * Корректное завершение: [Server.shutdown], ожидание, при таймауте — [Server.shutdownNow].
     */
    fun shutdown() {
        val s = server ?: return
        logger.info("gRPC server shutting down")
        s.shutdown()
        try {
            if (!s.awaitTermination(5, TimeUnit.SECONDS)) {
                s.shutdownNow()
                if (!s.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("gRPC server did not terminate")
                }
            }
        } catch (e: InterruptedException) {
            s.shutdownNow()
            Thread.currentThread().interrupt()
        } finally {
            server = null
        }
    }
}
