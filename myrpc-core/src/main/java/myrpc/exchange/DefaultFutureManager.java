package myrpc.exchange;

import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import myrpc.exchange.model.RpcRequest;
import myrpc.exchange.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DefaultFutureManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFutureManager.class);

    public static final Map<Long,DefaultFuture> DEFAULT_FUTURE_CACHE = new ConcurrentHashMap<>();
    public static final HashedWheelTimer TIMER = new HashedWheelTimer();

    public static void received(RpcResponse rpcResponse){
        Long messageId = rpcResponse.getMessageId();

        logger.debug("received rpcResponse={},DEFAULT_FUTURE_CACHE={}",rpcResponse,DEFAULT_FUTURE_CACHE);
        DefaultFuture defaultFuture = DEFAULT_FUTURE_CACHE.remove(messageId);

        if(defaultFuture != null){
            logger.debug("remove defaultFuture success");
            if(rpcResponse.getExceptionValue() != null){
                // 异常处理
                defaultFuture.completeExceptionally(rpcResponse.getExceptionValue());
            }else{
                // 正常返回
                defaultFuture.complete(rpcResponse);
            }
        }else{
            // 可能超时了，接到响应前已经remove掉了这个future(超时和实际接到请求都会调用received方法)
            logger.debug("remove defaultFuture fail");
        }
    }

    public static DefaultFuture createNewFuture(Channel channel, RpcRequest rpcRequest){
        DefaultFuture defaultFuture = new DefaultFuture(channel,rpcRequest);
        // 增加超时处理的逻辑
        newTimeoutCheck(defaultFuture);

        return defaultFuture;
    }

    public static DefaultFuture getFuture(long messageId){
        return DEFAULT_FUTURE_CACHE.get(messageId);
    }

    /**
     * 增加请求超时的检查任务
     * */
    public static void newTimeoutCheck(DefaultFuture defaultFuture){
        TimeoutCheckTask timeoutCheckTask = new TimeoutCheckTask(defaultFuture.getMessageId());
        TIMER.newTimeout(timeoutCheckTask,defaultFuture.getTimeout(), TimeUnit.MILLISECONDS);
    }
}
