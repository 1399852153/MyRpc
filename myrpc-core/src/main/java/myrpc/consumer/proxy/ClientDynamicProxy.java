package myrpc.consumer.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import myrpc.balance.LoadBalance;
import myrpc.common.config.GlobalConfig;
import myrpc.common.enums.MessageFlagEnums;
import myrpc.common.enums.MessageSerializeType;
import myrpc.common.model.*;
import myrpc.consumer.context.ConsumerRpcContextHolder;
import myrpc.exception.MyRpcException;
import myrpc.exception.MyRpcRemotingException;
import myrpc.exchange.DefaultFuture;
import myrpc.exchange.DefaultFutureManager;
import myrpc.common.util.JsonUtil;
import myrpc.exchange.model.MessageHeader;
import myrpc.exchange.model.MessageProtocol;
import myrpc.exchange.model.RpcRequest;
import myrpc.exchange.model.RpcResponse;
import myrpc.netty.client.NettyClient;
import myrpc.netty.client.NettyClientFactory;
import myrpc.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 客户端动态代理
 * */
public class ClientDynamicProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientDynamicProxy.class);

    private final Registry registry;
    private final LoadBalance loadBalance;

    public ClientDynamicProxy(Registry registry, LoadBalance loadBalance) {
        this.registry = registry;
        this.loadBalance = loadBalance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object localMethodResult = processLocalMethod(proxy,method,args);
        if(localMethodResult != null){
            // 处理toString等对象自带方法，不发起rpc调用
            return localMethodResult;
        }

        logger.debug("ClientDynamicProxy before: methodName=" + method.getName());

        String serviceName = method.getDeclaringClass().getName();
        List<ServiceInfo> serviceInfoList = registry.discovery(serviceName);
        logger.debug("serviceInfoList.size={},serviceInfoList={}",serviceInfoList.size(),JsonUtil.obj2Str(serviceInfoList));

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

        NettyClient nettyClient = getTargetClient(serviceInfoList);
        logger.info("ClientDynamicProxy getTargetClient={}", nettyClient);
        Channel channel = nettyClient.getChannel();

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

    private NettyClient getTargetClient(List<ServiceInfo> serviceInfoList){
        URLAddress targetProviderAddress = ConsumerRpcContextHolder.getConsumerRpcContext().getTargetProviderAddress();
        if(targetProviderAddress == null) {
            // 未强制指定被调用方地址，负载均衡获得调用的服务端(正常逻辑)
            ServiceInfo selectedServiceInfo = loadBalance.select(serviceInfoList);
            logger.debug("selected info = " + selectedServiceInfo.getUrlAddress());
            return NettyClientFactory.getNettyClient(selectedServiceInfo.getUrlAddress());
        }else{
            // 从注册服务的中找到指定的服务
            ServiceInfo targetServiceInfo = serviceInfoList.stream()
                .filter(item->item.getUrlAddress().equals(targetProviderAddress))
                .findAny()
                // 找不到，抛异常
                .orElseThrow(()->new MyRpcException("set targetProviderAddress，but can not find. targetProviderAddress=" + targetProviderAddress));
            return NettyClientFactory.getNettyClient(targetServiceInfo.getUrlAddress());
        }
    }
}
