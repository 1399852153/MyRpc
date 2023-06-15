package myrpc.demo.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.common.enums.MessageFlagEnums;
import myrpc.common.enums.MessageSerializeType;
import myrpc.common.model.MessageHeader;
import myrpc.common.model.MessageProtocol;
import myrpc.common.model.RpcRequest;
import myrpc.demo.common.model.User;
import myrpc.netty.message.codec.NettyDecoder;
import myrpc.netty.message.codec.NettyEncoder;

public class RpcClientNoProxy {

    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(8,
            new DefaultThreadFactory("NettyClientWorker", true));

        bootstrap.group(eventLoopGroup)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline()
                        // 编码、解码处理器
                        .addLast("encoder", new NettyEncoder<>())
                        .addLast("decoder", new NettyDecoder())
                        .addLast("clientHandler", new SimpleChannelInboundHandler<MessageProtocol>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageProtocol messageProtocol) {
                                System.out.println("PureNettyClient received messageProtocol=" + messageProtocol);
                            }
                        })
                    ;
                }
            });

        ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8888).sync();
        Channel channel = channelFuture.sync().channel();

        // 构造消息对象
        MessageProtocol<RpcRequest> messageProtocol = buildMessage();
        // 发送消息
        channel.writeAndFlush(messageProtocol);

        System.out.println("RpcClientNoProxy send request success!");
        channelFuture.channel().closeFuture().sync();
    }

    private static MessageProtocol<RpcRequest> buildMessage(){
        // 构造请求
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setInterfaceName("myrpc.demo.common.service.UserService");
        rpcRequest.setMethodName("getUserFriend");
        rpcRequest.setParameterClasses(new Class[]{User.class,String.class});

        User user = new User("Jerry",10);
        String messageArg = "hello hello!";
        rpcRequest.setParams(new Object[]{user,messageArg});

        // 构造协议头
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageFlag(MessageFlagEnums.REQUEST.getCode());
        messageHeader.setTwoWayFlag(false);
        messageHeader.setEventFlag(true);
        messageHeader.setSerializeType(MessageSerializeType.JSON.getCode());
        messageHeader.setMessageId(rpcRequest.getMessageId());

        return new MessageProtocol<>(messageHeader,rpcRequest);
    }
}
