package myrpc.consumer;

import io.netty.bootstrap.Bootstrap;
import myrpc.common.model.URLAddress;
import myrpc.consumer.proxy.ClientDynamicProxy;

import java.lang.reflect.Proxy;

/**
 * consumer
 * @author shanreng
 */
public class Consumer<T> {

    private final Class<?> interfaceClass;
    private final T proxy;

    private final Bootstrap bootstrap;
    private final URLAddress urlAddress;

    public Consumer(Class<?> interfaceClass, Bootstrap bootstrap, URLAddress urlAddress) {
        this.interfaceClass = interfaceClass;
        this.bootstrap = bootstrap;
        this.urlAddress = urlAddress;

        ClientDynamicProxy clientDynamicProxy = new ClientDynamicProxy(bootstrap,urlAddress);

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
