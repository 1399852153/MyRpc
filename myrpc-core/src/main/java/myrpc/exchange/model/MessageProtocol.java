package myrpc.exchange.model;

import java.io.Serializable;

/**
 * 完整的消息对象
 * */
public class MessageProtocol<T> implements Serializable {

    /**
     * 请求头
     * */
    private MessageHeader messageHeader;

    /**
     * 请求体(实际的业务消息对象)
     * */
    private T bizDataBody;

    public MessageProtocol(MessageHeader messageHeader, T bizDataBody) {
        this.messageHeader = messageHeader;
        this.bizDataBody = bizDataBody;
    }

    public MessageHeader getMessageHeader() {
        return messageHeader;
    }

    public T getBizDataBody() {
        return bizDataBody;
    }

    @Override
    public String toString() {
        return "MessageProtocol{" +
            "messageHeader=" + messageHeader +
            ", bizDataBody=" + bizDataBody +
            '}';
    }
}
