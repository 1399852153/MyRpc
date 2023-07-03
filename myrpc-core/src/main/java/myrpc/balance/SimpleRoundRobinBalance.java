package myrpc.balance;

import myrpc.common.model.ServiceInfo;
import myrpc.exception.MyRpcException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 无权重的轮训负载均衡（后续增加带权重的轮训）
 * */
public class SimpleRoundRobinBalance implements LoadBalance{

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfoList) {
        if(serviceInfoList.isEmpty()){
            throw new MyRpcException("serviceInfoList is empty!");
        }

        // 考虑一下溢出，取绝对值
        int selectedIndex = Math.abs(count.getAndIncrement());
        return serviceInfoList.get(selectedIndex % serviceInfoList.size());
    }
}
