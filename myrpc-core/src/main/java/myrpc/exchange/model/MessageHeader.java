package myrpc.exchange.model;

import myrpc.common.enums.MessageFlagEnums;
import myrpc.common.enums.MessageSerializeType;

import java.io.Serializable;

public class MessageHeader implements Serializable {

    public static final int MESSAGE_HEADER_LENGTH = 16;
    public static final int MESSAGE_SERIALIZE_TYPE_LENGTH = 5;
    public static final short MAGIC = (short)0x2233;

    // ================================ 消息头 =================================
    /**
     * 魔数(占2字节)
     * */
    private short magicNumber = MAGIC;

    /**
     * 消息标识(0代表请求事件；1代表响应事件， 占1位)
     * @see MessageFlagEnums
     * */
    private Boolean messageFlag;

    /**
     * 是否是双向请求(0代表oneWay请求；1代表twoWay请求）
     * （双向代表客户端会等待服务端的响应，单向则请求发送完成后即向上层返回成功)
     * */
    private Boolean twoWayFlag;

    /**
     * 是否是心跳消息(0代表正常消息；1代表心跳消息， 占1位)
     * */
    private Boolean eventFlag;

    /**
     * 消息体序列化类型(占5位，即所支持的序列化类型不得超过2的5次方，32种)
     * @see MessageSerializeType
     * */
    private Boolean[] serializeType;

    /**
     * 响应状态(占1字节)
     * */
    private byte responseStatus;

    /**
     * 消息的唯一id（占8字节）
     * */
    private long messageId;

    /**
     * 业务数据长度（占4字节）
     * */
    private int bizDataLength;

    public short getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(short magicNumber) {
        this.magicNumber = magicNumber;
    }

    public Boolean getMessageFlag() {
        return messageFlag;
    }

    public void setMessageFlag(Boolean messageFlag) {
        this.messageFlag = messageFlag;
    }

    public Boolean getTwoWayFlag() {
        return twoWayFlag;
    }

    public void setTwoWayFlag(Boolean twoWayFlag) {
        this.twoWayFlag = twoWayFlag;
    }

    public Boolean getEventFlag() {
        return eventFlag;
    }

    public void setEventFlag(Boolean eventFlag) {
        this.eventFlag = eventFlag;
    }

    public Boolean[] getSerializeType() {
        return serializeType;
    }

    public void setSerializeType(Boolean[] serializeType) {
        this.serializeType = serializeType;
    }

    public byte getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(byte responseStatus) {
        this.responseStatus = responseStatus;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public int getBizDataLength() {
        return bizDataLength;
    }

    public void setBizDataLength(int bizDataLength) {
        this.bizDataLength = bizDataLength;
    }
}
