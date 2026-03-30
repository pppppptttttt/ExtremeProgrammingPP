package org.example

import grpc.client.ChatGrpcClient
import grpc.client.echoProbe
import grpc.server.ChatGrpcServer

fun main(args: Array<String>) {
    if (args.size >= 2 && args[0] == "--server") {
        val port = args[1].toIntOrNull() ?: error("Invalid port: ${args[1]}")
        val bindHost = args.getOrElse(2) { "0.0.0.0" }
        val grpc = ChatGrpcServer(bindHost, port)
        grpc.start()
        Runtime.getRuntime().addShutdownHook(Thread { grpc.shutdown() })
        grpc.blockUntilShutdown()
        return
    }
    if (args.size >= 3 && args[0] == "--client") {
        val host = args[1]
        val port = args[2].toIntOrNull() ?: error("Invalid port: ${args[2]}")
        val client = ChatGrpcClient(host, port)
        client.connect()
        try {
            echoProbe(client, "smoke-client", "hello")
        } finally {
            client.shutdown()
        }
        return
    }
    println(
        "Usage:\n" +
            "  --server <port> [bindHost]     default bindHost=0.0.0.0\n" +
            "  --client <host> <port>         smoke echo to running server\n" +
            "Example: --server 50051\n" +
            "         --client 127.0.0.1 50051",
    )
}
