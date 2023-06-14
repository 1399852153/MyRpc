package myrpc.netty.message.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import myrpc.common.enums.MessageFlagEnums;
import myrpc.common.model.*;
import myrpc.netty.util.MessageCodecUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * netty 解码器
 * @author shanreng
 */
public class NettyDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(NettyDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list){

        // 保存读取前的读指针
        int beforeReadIndex = byteBuf.readerIndex();
        do{
            try {
                MessageDecodeResult messageDecodeResult = decodeHeader(byteBuf);

                if (messageDecodeResult.isNeedMoreData()) {
                    // 出现拆包没有读取到一个完整的rpc请求，还原byteBuf读指针，等待下一次读事件
                    byteBuf.readerIndex(beforeReadIndex);
                    break;
                } else {
                    // 正常解析完一个完整的message，交给后面的handler处理
                    list.add(messageDecodeResult.getMessageProtocol());
                }
            }catch (Exception e){
                // 比如decodeHeader里json序列化失败了等等.直接跳过这个数据包不还原了
                logger.error("NettyDecoder error!",e);
            }

            // 循环，直到整个ByteBuf读取完
        }while(byteBuf.isReadable());
    }

    /**
     * @return
     * 返回false代表还需要等待更多的数据，
     * 返回true表示正常
     * */
    private MessageDecodeResult decodeHeader(ByteBuf byteBuf){
        int readable = byteBuf.readableBytes();
        if(readable < MessageHeader.MESSAGE_HEADER_LENGTH){
            // 无法读取到一个完整的header，说明出现了拆包，等待更多的数据
            return MessageDecodeResult.needMoreData();
        }

        // 读取header头
        MessageHeader messageHeader = MessageCodecUtil.messageHeaderDecode(byteBuf);

        int bizDataLength = messageHeader.getBizDataLength();
        if(byteBuf.readableBytes() < bizDataLength){
            // 无法读取到一个完整的正文内容，说明出现了拆包，等待更多的数据
            return MessageDecodeResult.needMoreData();
        }

        // 基于消息类型标识，解析rpc正文对象
        boolean messageFlag = messageHeader.getMessageFlag();
        if(messageFlag == MessageFlagEnums.REQUEST.getCode()){
            RpcRequest rpcRequest = MessageCodecUtil.messageBizDataDecode(byteBuf,bizDataLength,RpcRequest.class);
            MessageProtocol<RpcRequest> messageProtocol = new MessageProtocol<>(messageHeader,rpcRequest);
            // 正确的解析完一个rpc请求消息
            return MessageDecodeResult.decodeSuccess(messageProtocol);
        }else{
            RpcResponse rpcResponse = MessageCodecUtil.messageBizDataDecode(byteBuf,bizDataLength,RpcResponse.class);
            MessageProtocol<RpcResponse> messageProtocol = new MessageProtocol<>(messageHeader,rpcResponse);
            // 正确的解析完一个rpc响应消息
            return MessageDecodeResult.decodeSuccess(messageProtocol);
        }
    }
}
