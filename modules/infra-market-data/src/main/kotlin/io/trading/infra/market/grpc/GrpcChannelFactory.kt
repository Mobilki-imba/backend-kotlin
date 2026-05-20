package io.trading.infra.market.grpc

import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import java.util.concurrent.TimeUnit

class GrpcChannelFactory(private val address: String) {
    val channel: ManagedChannel = NettyChannelBuilder.forTarget(address)
        .usePlaintext()
        .keepAliveTime(30, TimeUnit.SECONDS)
        .keepAliveTimeout(10, TimeUnit.SECONDS)
        .keepAliveWithoutCalls(true)
        .build()

    fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
