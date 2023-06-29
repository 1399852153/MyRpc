package myrpc.consumer;

import io.netty.bootstrap.Bootstrap;
import myrpc.common.model.URLAddress;
import myrpc.consumer.proxy.ClientDynamicProxy;
import myrpc.registry.Registry;

import java.lang.reflect.Proxy;

/**
 * consumer
 */
public class Consumer<T> {

    private final Class<?> interfaceClass;
    private final T proxy;


    public Consumer(Class<?> interfaceClass,  Registry registry) {
        this.interfaceClass = interfaceClass;

        ClientDynamicProxy clientDynamicProxy = new ClientDynamicProxy(registry);

        this.proxy = (T) Proxy.newProxyInstance(
                clientDynamicProxy.getClass().getClassLoader(),
                new Class[]{interfaceClass},
                clientDynamicProxy);
    }

    public T getProxy() {
        return proxy;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

}
