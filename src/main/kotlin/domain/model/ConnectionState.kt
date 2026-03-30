package domain.model

/** Состояние жизненного цикла соединения на стороне приложения (UI/домен). */
sealed interface ConnectionState {
    /** Нет активного соединения. */
    data object Idle : ConnectionState

    /** Слушаем порт, ждём входящего подключения. */
    data object Listening : ConnectionState

    /** Исходящее подключение к [peer] в процессе. */
    data class Connecting(
        val peer: PeerInfo,
    ) : ConnectionState

    /** Duplex-сессия с [peer] установлена. */
    data class Connected(
        val peer: PeerInfo,
    ) : ConnectionState

    /** Соединение не удалось; [reason] для пользователя или логов. */
    data class Failed(
        val reason: String,
    ) : ConnectionState
}
