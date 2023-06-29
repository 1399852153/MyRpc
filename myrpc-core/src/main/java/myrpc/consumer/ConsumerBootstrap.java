package myrpc.consumer;

import myrpc.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class ConsumerBootstrap {

    private Registry registry;
    private final Map<Class<?>,Consumer<?>> consumerMap = new HashMap<>();

    public ConsumerBootstrap registry(Registry registry){
        this.registry = registry;
        return this;
    }

    public <T> Consumer<T> registerConsumer(Class<T> clazz){
        Consumer<T> consumer = new Consumer<>(clazz,this.registry);
        consumerMap.put(clazz,consumer);
        return consumer;
    }
}
