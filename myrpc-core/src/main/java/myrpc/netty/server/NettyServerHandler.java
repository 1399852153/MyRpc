package myrpc.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import myrpc.common.enums.MessageFlagEnums;
import myrpc.exchange.model.MessageHeader;
import myrpc.exchange.model.MessageProtocol;
import myrpc.exchange.model.RpcRequest;
import myrpc.exchange.model.RpcResponse;
import myrpc.provider.Provider;
import myrpc.provider.ProviderManager;
import myrpc.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NettyServerHandler extends SimpleChannelInboundHandler<MessageProtocol<RpcRequest>> {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    /**
     * 处理实际的业务请求的线程池(简单起见，直接写死参数配置)
     * */
    private final ThreadPoolExecutor bizTaskExecutor =
            new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000));


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageProtocol<RpcRequest> rpcRequestMessageProtocol){
        logger.debug("NettyServerHandler channelRead0={}", JsonUtil.obj2Str(rpcRequestMessageProtocol));

        // 用线程池处理实际的业务请求,避免耗时业务操作阻塞NIO事件循环
        bizTaskExecutor.execute(()->{
            MessageProtocol<RpcResponse> responseMessage = getResponseMessage(rpcRequestMessageProtocol);
            logger.debug("NettyServerHandler write responseMessage={}", JsonUtil.obj2Str(responseMessage));
            channelHandlerContext.channel().writeAndFlush(responseMessage);
        });

    }

    private MessageProtocol<RpcResponse> getResponseMessage(MessageProtocol<RpcRequest> rpcRequestMessageProtocol){
        long requestMessageId = rpcRequestMessageProtocol.getMessageHeader().getMessageId();

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageId(requestMessageId);
        messageHeader.setMessageFlag(MessageFlagEnums.RESPONSE.getCode());
        messageHeader.setTwoWayFlag(false);
        messageHeader.setEventFlag(false);
        // 使用相同的序列化类型
        messageHeader.setSerializeType(rpcRequestMessageProtocol.getMessageHeader().getSerializeType());
        messageHeader.setResponseStatus((byte)'a');

        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessageId(requestMessageId);

        try {
            // 反射调用具体的实现类
            Object result = invokeTargetService(rpcRequestMessageProtocol.getBizDataBody());

            // 设置返回值
            rpcResponse.setReturnValue(result);
        }catch (Exception e){
            logger.warn("invokeTargetService error",e);

            // 调用具体实现类时，出现异常，设置异常的值
            rpcResponse.setExceptionValue(e);
        }

        return new MessageProtocol<>(messageHeader,rpcResponse);
    }

    private Object invokeTargetService(RpcRequest rpcRequest) throws Exception {
        String interfaceClassName = rpcRequest.getInterfaceName();

        // 从provider的缓存中获得
        Provider provider = ProviderManager.getProvider(interfaceClassName);
        Object ref = provider.getRef();
        final Method method = ref.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterClasses());

        Object result = method.invoke(ref, rpcRequest.getParams());

        return result;
    }
}
