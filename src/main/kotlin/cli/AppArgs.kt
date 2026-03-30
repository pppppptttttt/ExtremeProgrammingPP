package cli

import domain.model.PeerInfo

/** Результат разбора CLI: локальное имя пользователя и режим запуска. */
data class AppArgs(
    val selfName: String,
    val mode: LaunchMode,
)

/** Режим процесса: слушать [listenPort] или подключаться к [peer]. */
sealed interface LaunchMode {
    /** Ожидание входящего gRPC-подключения на [listenPort]. */
    data class Server(
        val listenPort: Int,
    ) : LaunchMode

    /** Исходящее подключение к [peer]. */
    data class Client(
        val peer: PeerInfo,
    ) : LaunchMode
}
