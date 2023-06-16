package myrpc.exchange.model;

import java.io.Serializable;

public class RpcResponse implements Serializable {

    /**
     * 消息的唯一id（占8字节）
     * */
    private long messageId;

    /**
     * 返回值
     */
    private Object returnValue;

    /**
     * 异常值
     */
    private Exception exceptionValue;

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public Exception getExceptionValue() {
        return exceptionValue;
    }

    public void setExceptionValue(Exception exceptionValue) {
        this.exceptionValue = exceptionValue;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
            "messageId=" + messageId +
            ", returnValue=" + returnValue +
            ", exceptionValue=" + exceptionValue +
            '}';
    }
}
