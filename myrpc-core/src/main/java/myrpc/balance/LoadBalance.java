package myrpc.balance;

import myrpc.common.model.ServiceInfo;

import java.util.List;

/**
 * 负载均衡选择器
 * */
public interface LoadBalance {

    ServiceInfo select(List<ServiceInfo> serviceInfoList);
}
