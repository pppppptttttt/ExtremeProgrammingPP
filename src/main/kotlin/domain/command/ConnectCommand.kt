package domain.command

import domain.model.PeerInfo

/** Команда исходящего подключения к [peer] от имени [selfName]. */
data class ConnectCommand(
    val selfName: String,
    val peer: PeerInfo,
)
