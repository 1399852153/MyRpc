package myrpc.exchange;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import myrpc.exception.MyRpcTimeoutException;
import myrpc.exchange.model.RpcResponse;

public class TimeoutCheckTask implements TimerTask {

    private final long messageId;

    public TimeoutCheckTask(long messageId) {
        this.messageId = messageId;
    }

    @Override
    public void run(Timeout timeout) {
        DefaultFuture defaultFuture = DefaultFutureManager.getFuture(this.messageId);
        if(defaultFuture == null || defaultFuture.isDone()){
            // 请求已经在超时前返回，处理过了,直接返回即可
            return;
        }

        // 构造超时的响应
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessageId(this.messageId);
        rpcResponse.setExceptionValue(new MyRpcTimeoutException(
            "request timeout：" + defaultFuture.getTimeout() + " channel=" + defaultFuture.getChannel()));

        DefaultFutureManager.received(rpcResponse);
    }
}
