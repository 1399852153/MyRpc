package myrpc.registry;

import myrpc.common.model.ServiceInfo;

import java.util.List;

/**
 * 注册中心的抽象
 * */
public interface Registry {

    String ZK_BASE_PATH = "/my_rpc";

    /**
     * 服务注册
     * */
    void doRegistry(ServiceInfo serviceInfo);

    /**
     * 服务发现
     * */
    List<ServiceInfo> discovery(String serviceName);
}
