package myrpc.consumer;

import myrpc.balance.LoadBalance;
import myrpc.balance.SimpleRoundRobinBalance;
import myrpc.invoker.Invoker;
import myrpc.invoker.impl.FastFailInvoker;
import myrpc.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class ConsumerBootstrap {

    private Registry registry;
    private LoadBalance loadBalance = new SimpleRoundRobinBalance();
    private Invoker invoker = new FastFailInvoker();

    private final Map<Class<?>,Consumer<?>> consumerMap = new HashMap<>();

    public ConsumerBootstrap registry(Registry registry){
        this.registry = registry;
        return this;
    }

    public ConsumerBootstrap loadBalance(LoadBalance loadBalance){
        this.loadBalance = loadBalance;
        return this;
    }

    public ConsumerBootstrap invoker(Invoker invoker){
        this.invoker = invoker;
        return this;
    }

    public <T> Consumer<T> registerConsumer(Class<T> clazz){
        Consumer<T> consumer = new Consumer<>(clazz,this.registry,this.loadBalance,this.invoker);
        consumerMap.put(clazz,consumer);
        return consumer;
    }
}
