package myrpc.common.model;

public class MessageProtocol<T> {

    private MessageHeader messageHeader;
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
