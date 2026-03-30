package domain.command

import domain.model.PeerInfo

data class ConnectCommand(
    val selfName: String, // username
    val peer: PeerInfo
)
