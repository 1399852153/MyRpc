package myrpc.common.model;

import myrpc.exchange.model.MessageProtocol;

public class MessageDecodeResult {

    private boolean needMoreData;
    private MessageProtocol messageProtocol;

    private MessageDecodeResult(boolean needMoreData) {
        this.needMoreData = needMoreData;
    }

    private MessageDecodeResult(boolean needMoreData, MessageProtocol messageProtocol) {
        this.needMoreData = needMoreData;
        this.messageProtocol = messageProtocol;
    }

    public static MessageDecodeResult needMoreData(){
        return new MessageDecodeResult(true);
    }

    public static MessageDecodeResult decodeSuccess(MessageProtocol message){
        return new MessageDecodeResult(false,message);
    }

    public boolean isNeedMoreData() {
        return needMoreData;
    }

    public MessageProtocol getMessageProtocol() {
        return messageProtocol;
    }
}
