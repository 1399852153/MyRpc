package myrpc.consumer;

import myrpc.balance.LoadBalance;
import myrpc.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class ConsumerBootstrap {

    private Registry registry;
    private LoadBalance loadBalance;
    private final Map<Class<?>,Consumer<?>> consumerMap = new HashMap<>();

    public ConsumerBootstrap registry(Registry registry){
        this.registry = registry;
        return this;
    }

    public ConsumerBootstrap loadBalance(LoadBalance loadBalance){
        this.loadBalance = loadBalance;
        return this;
    }

    public <T> Consumer<T> registerConsumer(Class<T> clazz){
        Consumer<T> consumer = new Consumer<>(clazz,this.registry,this.loadBalance);
        consumerMap.put(clazz,consumer);
        return consumer;
    }
}
