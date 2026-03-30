package grpc.interceptor

import domain.model.NetworkPorts
import domain.model.PeerInfo
import io.grpc.Attributes
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Grpc
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * Кладёт remote-адрес TCP в [io.grpc.Context] до вызова `ChatService/chat`.
 */
class PeerAddressInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val remote: SocketAddress? =
            call.attributes.get(
                Grpc.TRANSPORT_ATTR_REMOTE_ADDR as Attributes.Key<SocketAddress>,
            )
        val peer = peerFrom(remote as? InetSocketAddress)
        val ctx = Context.current().withValue(GrpcPeerContext.PEER_INFO, peer)
        return Contexts.interceptCall(ctx, call, headers, next)
    }

    private fun peerFrom(addr: InetSocketAddress?): PeerInfo {
        if (addr == null) {
            return PeerInfo("peer", NetworkPorts.MIN_TCP_PORT)
        }
        val host = addr.hostString ?: addr.address?.hostAddress?.takeIf { it.isNotBlank() } ?: "peer"
        val port =
            addr.port.takeIf { it in NetworkPorts.MIN_TCP_PORT..NetworkPorts.MAX_TCP_PORT }
                ?: NetworkPorts.MIN_TCP_PORT
        return PeerInfo(host, port)
    }
}
