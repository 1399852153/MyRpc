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
