package myrpc.invoker;

import myrpc.exchange.model.RpcResponse;
import myrpc.netty.client.NettyClient;

public interface InvokerCallable {

    RpcResponse invoke(NettyClient nettyClient);
}
