package grpc

/** Общие таймауты gRPC (shutdown, зонды). */
object GrpcConstants {
    const val SHUTDOWN_AWAIT_SECONDS = 5L
    const val ECHO_PROBE_TIMEOUT_SECONDS = 15L
}
