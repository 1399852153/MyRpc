package myrpc.consumer.context;

public class ConsumerRpcContextHolder {

    private static final ThreadLocal<ConsumerRpcContext> consumerRpcContextThreadLocal =
            ThreadLocal.withInitial(ConsumerRpcContext::new);

    public static ConsumerRpcContext getConsumerRpcContext(){
        return consumerRpcContextThreadLocal.get();
    }

    public static void removeConsumerRpcContext(){
        consumerRpcContextThreadLocal.remove();
    }
}
