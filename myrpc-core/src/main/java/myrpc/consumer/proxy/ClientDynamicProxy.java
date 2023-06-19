package myrpc.consumer.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import myrpc.common.config.GlobalConfig;
import myrpc.common.enums.MessageFlagEnums;
import myrpc.common.enums.MessageSerializeType;
import myrpc.common.model.*;
import myrpc.exception.MyRpcRemotingException;
import myrpc.exchange.DefaultFuture;
import myrpc.exchange.DefaultFutureManager;
import myrpc.common.util.JsonUtil;
import myrpc.exchange.model.MessageHeader;
import myrpc.exchange.model.MessageProtocol;
import myrpc.exchange.model.RpcRequest;
import myrpc.exchange.model.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 客户端动态代理
 * */
public class ClientDynamicProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientDynamicProxy.class);

    private final Bootstrap bootstrap;
    private final URLAddress urlAddress;

    public ClientDynamicProxy(Bootstrap bootstrap, URLAddress urlAddress) {
        this.bootstrap = bootstrap;
        this.urlAddress = urlAddress;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object localMethodResult = processLocalMethod(proxy,method,args);
        if(localMethodResult != null){
            // 处理toString等对象自带方法，不发起rpc调用
            return localMethodResult;
        }

        logger.debug("ClientDynamicProxy before: methodName=" + method.getName());

        // 构造请求和协议头
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setInterfaceName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterClasses(method.getParameterTypes());
        rpcRequest.setParams(args);

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageFlag(MessageFlagEnums.REQUEST.getCode());
        messageHeader.setTwoWayFlag(false);
        messageHeader.setEventFlag(true);
        messageHeader.setSerializeType(GlobalConfig.messageSerializeType.getCode());
        messageHeader.setResponseStatus((byte)'a');
        messageHeader.setMessageId(rpcRequest.getMessageId());

        logger.debug("ClientDynamicProxy rpcRequest={}", JsonUtil.obj2Str(rpcRequest));

        ChannelFuture channelFuture = bootstrap.connect(urlAddress.getHost(),urlAddress.getPort()).sync();
        Channel channel = channelFuture.sync().channel();
        // 通过Promise，将netty的异步转为同步,参考dubbo DefaultFuture
        DefaultFuture<RpcResponse> defaultFuture = DefaultFutureManager.createNewFuture(channel,rpcRequest);

        channel.writeAndFlush(new MessageProtocol<>(messageHeader,rpcRequest));
        logger.debug("ClientDynamicProxy writeAndFlush success, wait result");

        // 调用方阻塞在这里
        RpcResponse rpcResponse = defaultFuture.get();

        logger.debug("ClientDynamicProxy defaultFuture.get() rpcResponse={}",rpcResponse);

        return processRpcResponse(rpcResponse);
    }

    private Object processLocalMethod(Object proxy, Method method, Object[] args) throws Exception {
        // 处理toString等对象自带方法，不发起rpc调用
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(proxy, args);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return proxy.toString();
            } else if ("hashCode".equals(methodName)) {
                return proxy.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return proxy.equals(args[0]);
        }

        // 返回null标识非本地方法，需要进行rpc调用
        return null;
    }

    private Object processRpcResponse(RpcResponse rpcResponse){
        if(rpcResponse.getExceptionValue() == null){
            // 没有异常，return正常的返回值
            return rpcResponse.getReturnValue();
        }else{
            // 有异常，往外抛出去
            throw new MyRpcRemotingException(rpcResponse.getExceptionValue());
        }
    }
}