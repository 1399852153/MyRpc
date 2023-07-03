package myrpc.balance;

import myrpc.common.model.ServiceInfo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 无权重，纯随机的负载均衡选择器
 * */
public class RandomLoadBalance implements LoadBalance{
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfoList) {
        int selectedIndex = ThreadLocalRandom.current().nextInt(serviceInfoList.size());
        return serviceInfoList.get(selectedIndex);
    }
}
