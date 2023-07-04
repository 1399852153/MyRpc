package myrpc.invoker.impl;

import myrpc.balance.LoadBalance;
import myrpc.common.model.ServiceInfo;
import myrpc.common.util.JsonUtil;
import myrpc.exchange.model.RpcResponse;
import myrpc.invoker.Invoker;
import myrpc.invoker.InvokerCallable;
import myrpc.invoker.InvokerUtil;
import myrpc.netty.client.NettyClient;
import myrpc.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 快速失败，调用1次就返回
 * */
public class FastFailInvoker implements Invoker {

    private static final Logger logger = LoggerFactory.getLogger(FastFailInvoker.class);

    @Override
    public RpcResponse invoke(InvokerCallable callable, String serviceName,
                              Registry registry, LoadBalance loadBalance) {
        List<ServiceInfo> serviceInfoList = registry.discovery(serviceName);
        logger.debug("serviceInfoList.size={},serviceInfoList={}",serviceInfoList.size(), JsonUtil.obj2Str(serviceInfoList));
        NettyClient nettyClient = InvokerUtil.getTargetClient(serviceInfoList,loadBalance);
        logger.info("ClientDynamicProxy getTargetClient={}", nettyClient);

        // fast-fail，简单的调用一次就行，有错误就直接向上抛
        return callable.invoke(nettyClient);
    }
}
