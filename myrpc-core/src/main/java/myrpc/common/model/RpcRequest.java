package myrpc.common.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * rpc请求对象
 * */
public class RpcRequest {

    private static final AtomicLong INVOKE_ID = new AtomicLong(0);

    /**
     * 消息的唯一id（占8字节）
     * */
    private final long messageId;

    /**
     * 接口名
     * */
    private String interfaceName;

    /**
     * 方法名
     * */
    private String methodName;

    /**
     * 参数类型数组(每个参数一项)
     * */
    private Class<?>[] parameterClasses;

    /**
     * 实际参数对象数组(每个参数一项)
     * */
    private Object[] params;

    public RpcRequest() {
        // 每个请求对象生成时都自动生成单机全局唯一的自增id
        this.messageId = INVOKE_ID.getAndIncrement();
    }

    public long getMessageId() {
        return messageId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParameterClasses() {
        return parameterClasses;
    }

    public void setParameterClasses(Class<?>[] parameterClasses) {
        this.parameterClasses = parameterClasses;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }
}
