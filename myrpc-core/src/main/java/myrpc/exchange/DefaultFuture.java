package myrpc.exchange;

import io.netty.channel.Channel;
import myrpc.exchange.model.RpcRequest;

import java.util.concurrent.CompletableFuture;

/**
 * 模仿dubbo DefaultFuture
 * */
public class DefaultFuture<T> extends CompletableFuture<T> {

    private final Channel channel;
    private final RpcRequest rpcRequest;

    public DefaultFuture(Channel channel, RpcRequest rpcRequest) {
        this.channel = channel;
        this.rpcRequest = rpcRequest;

        // 把当前future放入全局缓存中
        DefaultFutureManager.DEFAULT_FUTURE_CACHE.put(rpcRequest.getMessageId(),this);
    }

    public long getMessageId(){
        return this.rpcRequest.getMessageId();
    }

    public Channel getChannel() {
        return channel;
    }
}
