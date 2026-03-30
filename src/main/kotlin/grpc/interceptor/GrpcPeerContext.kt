package grpc.interceptor

import domain.model.PeerInfo
import io.grpc.Context

/**
 * Ключ [Context] для адреса подключившегося к серверу peer’а (устанавливается [PeerAddressInterceptor]).
 */
object GrpcPeerContext {
    val PEER_INFO: Context.Key<PeerInfo> = Context.key("peer-info")
}
