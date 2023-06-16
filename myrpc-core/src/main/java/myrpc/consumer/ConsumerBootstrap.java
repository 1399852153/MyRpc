package myrpc.consumer;

import io.netty.bootstrap.Bootstrap;
import myrpc.common.model.URLAddress;
import myrpc.exception.MyRpcException;

import java.util.HashMap;
import java.util.Map;

public class ConsumerBootstrap {

    private final Map<Class<?>,Consumer<?>> consumerMap = new HashMap<>();
    private final Bootstrap bootstrap;
    private final URLAddress urlAddress;

    public ConsumerBootstrap(Bootstrap bootstrap, URLAddress urlAddress) {
        this.bootstrap = bootstrap;
        this.urlAddress = urlAddress;
    }

    public <T> Consumer<T> registerConsumer(Class<T> clazz){
        if(!consumerMap.containsKey(clazz)){
            Consumer<T> consumer = new Consumer<>(clazz,this.bootstrap,this.urlAddress);
            consumerMap.put(clazz,consumer);
            return consumer;
        }

        throw new MyRpcException("duplicate consumer! clazz=" + clazz);
    }
}
