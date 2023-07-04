package myrpc.invoker;

import myrpc.balance.LoadBalance;
import myrpc.exchange.DefaultFuture;
import myrpc.exchange.model.RpcResponse;
import myrpc.registry.Registry;

public interface Invoker {

    RpcResponse invoke(InvokerCallable callable, String serviceName,
                                      Registry registry, LoadBalance loadBalance);
}
