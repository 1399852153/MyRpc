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
import myrpc.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 故障转移调用(如果调用出现了错误，则重试指定次数)
 * 1 如果重试过程中成功了，则快读返回
 * 2 如果重试了指定次数后还是没成功，则抛出异常
 * */
public class FailoverInvoker implements Invoker {

    private static final Logger logger = LoggerFactory.getLogger(FailoverInvoker.class);

    private final int defaultRetryCount = 2;
    private final int retryCount;

    public FailoverInvoker() {
        this.retryCount = defaultRetryCount;
    }

    public FailoverInvoker(int retryCount) {
        this.retryCount = Math.max(retryCount,1);
    }

    @Override
    public RpcResponse invoke(InvokerCallable callable, String serviceName, Registry registry, LoadBalance loadBalance) {
        MyRpcException myRpcException = null;

        for(int i=0; i<retryCount; i++){
            List<ServiceInfo> serviceInfoList = registry.discovery(serviceName);
            logger.debug("serviceInfoList.size={},serviceInfoList={}",serviceInfoList.size(), JsonUtil.obj2Str(serviceInfoList));
            NettyClient nettyClient = InvokerUtil.getTargetClient(serviceInfoList,loadBalance);
            logger.debug("ClientDynamicProxy getTargetClient={}", nettyClient);

            try {
                RpcResponse rpcResponse = callable.invoke(nettyClient);
                if(myRpcException != null){
                    // 虽然最终重试成功了，但是之前请求失败过
                    logger.warn("FailRetryInvoker finally success, but there have been failed providers");
                }
                return rpcResponse;
            }catch (Exception e){
                myRpcException = new MyRpcException(e);

                logger.warn("FailRetryInvoker callable.invoke error",e);
            }
        }

        // 走到这里说明经过了retryCount次重试依然不成功，myRpcException一定不为null
        throw myRpcException;
    }
}
