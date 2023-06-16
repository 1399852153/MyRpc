package myrpc.netty.util;

import io.netty.buffer.ByteBuf;
import myrpc.common.config.GlobalConfig;
import myrpc.exchange.model.MessageHeader;
import myrpc.exchange.model.MessageProtocol;
import myrpc.serialize.MyRpcSerializer;
import myrpc.serialize.SerializerManager;


public class MessageCodecUtil {

    /**
     * 报文协议编码
     * */
    public static <T> void messageEncode(MessageProtocol<T> messageProtocol, ByteBuf byteBuf) {
        MessageHeader messageHeader = messageProtocol.getMessageHeader();
        // 写入魔数
        byteBuf.writeShort(MessageHeader.MAGIC);

        // 写入消息标识
        byteBuf.writeBoolean(messageHeader.getMessageFlag());
        // 写入单/双向标识
        byteBuf.writeBoolean(messageHeader.getTwoWayFlag());
        // 写入消息事件标识
        byteBuf.writeBoolean(messageHeader.getEventFlag());
        // 写入序列化类型
        for(boolean b : messageHeader.getSerializeType()){
            byteBuf.writeBoolean(b);
        }
        // 写入响应状态
        byteBuf.writeByte(messageHeader.getResponseStatus());
        // 写入消息uuid
        byteBuf.writeLong(messageHeader.getMessageId());

        // 序列化消息体
        MyRpcSerializer myRpcSerializer = SerializerManager.getSerializer(messageHeader.getSerializeType());
        byte[] bizMessageBytes = myRpcSerializer.serialize(messageProtocol.getBizDataBody());
        // 获得并写入消息正文长度
        byteBuf.writeInt(bizMessageBytes.length);
        // 写入消息正文内容
        byteBuf.writeBytes(bizMessageBytes);
    }

    /**
     * 报文协议header头解码
     * */
    public static MessageHeader messageHeaderDecode(ByteBuf byteBuf){
        MessageHeader messageHeader = new MessageHeader();
        // 读取魔数
        messageHeader.setMagicNumber(byteBuf.readShort());
        // 读取消息标识
        messageHeader.setMessageFlag(byteBuf.readBoolean());
        // 读取单/双向标识
        messageHeader.setTwoWayFlag(byteBuf.readBoolean());
        // 读取消息事件标识
        messageHeader.setEventFlag(byteBuf.readBoolean());

        // 读取序列化类型
        Boolean[] serializeTypeBytes = new Boolean[MessageHeader.MESSAGE_SERIALIZE_TYPE_LENGTH];
        for(int i=0; i<MessageHeader.MESSAGE_SERIALIZE_TYPE_LENGTH; i++){
            serializeTypeBytes[i] = byteBuf.readBoolean();
        }
        messageHeader.setSerializeType(serializeTypeBytes);

        // 读取响应状态
        messageHeader.setResponseStatus(byteBuf.readByte());
        // 读取消息uuid
        messageHeader.setMessageId(byteBuf.readLong());

        // 读取消息正文长度
        int bizDataLength = byteBuf.readInt();
        messageHeader.setBizDataLength(bizDataLength);

        return messageHeader;
    }

    /**
     * 报文协议正文body解码
     * */
    public static <T> T messageBizDataDecode(MessageHeader messageHeader, ByteBuf byteBuf, Class<T> messageBizDataType){
        // 读取消息正文
        byte[] bizDataBytes = new byte[messageHeader.getBizDataLength()];
        byteBuf.readBytes(bizDataBytes);

        // 反序列化消息体
        MyRpcSerializer myRpcSerializer = SerializerManager.getSerializer(messageHeader.getSerializeType());
        return (T) myRpcSerializer.deserialize(bizDataBytes,messageBizDataType);
    }
}
