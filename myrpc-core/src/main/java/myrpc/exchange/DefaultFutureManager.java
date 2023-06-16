package myrpc.exchange;

import io.netty.channel.Channel;
import myrpc.exchange.model.RpcRequest;
import myrpc.exchange.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultFutureManager {

    private static Logger logger = LoggerFactory.getLogger(DefaultFutureManager.class);

    public static final Map<Long,DefaultFuture> DEFAULT_FUTURE_CACHE = new ConcurrentHashMap<>();

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
            logger.debug("remove defaultFuture fail");
        }
    }

    public static DefaultFuture createNewFuture(Channel channel, RpcRequest rpcRequest){
        DefaultFuture defaultFuture = new DefaultFuture(channel,rpcRequest);

        return defaultFuture;
    }

    public static DefaultFuture getFuture(long messageId){
        return DEFAULT_FUTURE_CACHE.get(messageId);
    }
}
