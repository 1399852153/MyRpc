package myrpc.exchange;

import io.netty.channel.Channel;
import myrpc.exchange.model.RpcRequest;

import java.util.concurrent.CompletableFuture;

/**
 * 模仿dubbo DefaultFuture
 * */
public class DefaultFuture<T> extends CompletableFuture<T> {

    /**
     * 默认的超时时间(毫秒)
     * */
    public static final long DEFAULT_TIME_OUT = 1000;

    private final Channel channel;
    private final RpcRequest rpcRequest;
    private final long timeout;

    public DefaultFuture(Channel channel, RpcRequest rpcRequest) {
        this(channel,rpcRequest,DEFAULT_TIME_OUT);
    }

    public DefaultFuture(Channel channel, RpcRequest rpcRequest, long timeout) {
        this.channel = channel;
        this.rpcRequest = rpcRequest;
        this.timeout = timeout;

        // 把当前future放入全局缓存中
        DefaultFutureManager.DEFAULT_FUTURE_CACHE.put(rpcRequest.getMessageId(),this);
    }

    public long getTimeout() {
        return this.timeout;
    }

    public long getMessageId(){
        return this.rpcRequest.getMessageId();
    }

    public Channel getChannel() {
        return channel;
    }
}
