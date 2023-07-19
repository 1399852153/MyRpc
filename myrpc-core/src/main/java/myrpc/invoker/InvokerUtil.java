package myrpc.invoker;

import myrpc.balance.LoadBalance;
import myrpc.common.model.ServiceInfo;
import myrpc.common.model.URLAddress;
import myrpc.consumer.context.ConsumerRpcContextHolder;
import myrpc.exception.MyRpcException;
import myrpc.netty.client.NettyClient;
import myrpc.netty.client.NettyClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InvokerUtil {

    private static final Logger logger = LoggerFactory.getLogger(InvokerUtil.class);


    public static NettyClient getTargetClient(List<ServiceInfo> serviceInfoList, LoadBalance loadBalance){
        URLAddress targetProviderAddress = ConsumerRpcContextHolder.getConsumerRpcContext().getTargetProviderAddress();
        if(targetProviderAddress == null) {
            // 未强制指定被调用方地址，负载均衡获得调用的服务端(正常逻辑)
            ServiceInfo selectedServiceInfo = loadBalance.select(serviceInfoList);
            logger.debug("selected info = " + selectedServiceInfo.getUrlAddress());
            return NettyClientFactory.getNettyClient(selectedServiceInfo.getUrlAddress());
        }else{
            // 用于点对点的rpc，直接用上下文里的地址
            return NettyClientFactory.getNettyClient(targetProviderAddress);
        }
    }
}
