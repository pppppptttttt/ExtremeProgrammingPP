package domain.port

import domain.command.ConnectCommand
import domain.command.SendMessageCommand
import domain.command.StartServerCommand
import domain.model.ChatEvent
import domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatTransport : AutoCloseable {
    /**
     * Поток входящих событий: подключение, отключение, входящие сообщения, системные ошибки.
     */
    val events: Flow<ChatEvent>

    /**
     * Переходит в режим ожидания входящего подключения и возвращает управление сразу после старта listener'а.
     */
    suspend fun startServer(command: StartServerCommand)

    /**
     * Устанавливает исходящее подключение к peer'у.
     */
    suspend fun connect(command: ConnectCommand)

    /**
     * Отправляет сообщение и возвращает уже сформированное доменное сообщение
     * с тем sentAt/id, которые реально ушли в транспорт.
     */
    suspend fun send(command: SendMessageCommand): ChatMessage

    /**
     * Корректно завершает соединение/сервер.
     */
    suspend fun disconnect()

    override fun close() = Unit
}
