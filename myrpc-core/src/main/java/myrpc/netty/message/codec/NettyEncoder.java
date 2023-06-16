package myrpc.netty.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import myrpc.exchange.model.MessageProtocol;
import myrpc.netty.util.MessageCodecUtil;

public class NettyEncoder<T> extends MessageToByteEncoder<MessageProtocol<T>> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MessageProtocol<T> messageProtocol, ByteBuf byteBuf) {
        // 继承自MessageToByteEncoder中，只需要将编码后的数据写入参数中指定的byteBuf中即可
        // MessageToByteEncoder源码逻辑中会自己去将byteBuf写入channel的
        MessageCodecUtil.messageEncode(messageProtocol,byteBuf);
    }
}
