package myrpc.invoker.impl;

import myrpc.balance.LoadBalance;
import myrpc.common.model.ServiceInfo;
import myrpc.common.util.JsonUtil;
import myrpc.exception.MyRpcException;
import myrpc.exchange.model.RpcResponse;
import myrpc.invoker.Invoker;
import myrpc.invoker.InvokerCallable;
import myrpc.invoker.InvokerUtil;
import myrpc.netty.client.NettyClient;
import myrpc.netty.client.NettyClientFactory;
import myrpc.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 广播调用，如果有一个provider出错，就整体返回异常
 * */
public class BroadcastInvoker implements Invoker {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastInvoker.class);

    @Override
    public RpcResponse invoke(InvokerCallable callable, String serviceName, Registry registry, LoadBalance loadBalance) {
        List<ServiceInfo> serviceInfoList = registry.discovery(serviceName);
        logger.debug("serviceInfoList.size={},serviceInfoList={}",serviceInfoList.size(), JsonUtil.obj2Str(serviceInfoList));

        RpcResponse finallyRpcResponse = null;
        MyRpcException myRpcException = null;
        for(ServiceInfo serviceInfo : serviceInfoList){
            NettyClient nettyClient = NettyClientFactory.getNettyClient(serviceInfo.getUrlAddress());

            try {
                RpcResponse rpcResponse = callable.invoke(nettyClient);

                finallyRpcResponse = rpcResponse;
            }catch (Exception e){
                myRpcException = new MyRpcException(e);

                logger.warn("FailRetryInvoker callable.invoke error",e);
            }
        }

        if(myRpcException != null){
            // 存在至少一个provider异常，整体返回异常
            throw myRpcException;
        }else{
            // 所有provider都正常的返回了，返回最后一个provider的结果
            return finallyRpcResponse;
        }
    }
}
