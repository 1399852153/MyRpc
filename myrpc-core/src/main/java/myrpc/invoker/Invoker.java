package myrpc.invoker;

import myrpc.balance.LoadBalance;
import myrpc.exchange.model.RpcResponse;
import myrpc.registry.Registry;

/**
 * 不同的集群调用方式
 * */
public interface Invoker {

    RpcResponse invoke(InvokerCallable callable, String serviceName,
                                      Registry registry, LoadBalance loadBalance);
}
