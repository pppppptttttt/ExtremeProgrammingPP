package grpc.server

import io.grpc.stub.StreamObserver
import org.hse.chat.v1.ChatMessage
import org.hse.chat.v1.ChatServiceGrpc

/**
 * Заглушка: входящие [ChatMessage] из клиентского потока отражаются в ответный поток (эхо).
 */
class EchoChatService : ChatServiceGrpc.ChatServiceImplBase() {
    override fun chat(responseObserver: StreamObserver<ChatMessage>): StreamObserver<ChatMessage> =
        object : StreamObserver<ChatMessage> {
            override fun onNext(value: ChatMessage) {
                responseObserver.onNext(value)
            }

            override fun onError(t: Throwable) {
                responseObserver.onError(t)
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }
        }
}
