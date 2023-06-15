package myrpc.demo.purenetty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.demo.common.model.User;
import myrpc.netty.message.codec.NettyDecoder;
import myrpc.netty.message.codec.NettyEncoder;
import myrpc.serialize.json.JsonUtil;

/**
 * 最原始的netty客户端demo
 * */
public class PureNettyClient {

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
                        .addLast("clientHandler", new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf responseByteBuf) {
                                String responseStr = responseByteBuf.toString(CharsetUtil.UTF_8);
                                System.out.println("PureNettyClient received response=" + responseStr);
                            }
                        })
                    ;
                }
            });

        ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8888).sync();
        Channel channel = channelFuture.sync().channel();

        // 发送一个user对象的json串
        User user = new User("Tom",10);
        ByteBuf requestByteBuf = Unpooled.copiedBuffer(JsonUtil.obj2Str(user), CharsetUtil.UTF_8);
        channel.writeAndFlush(requestByteBuf);

        System.out.println("netty client send request success!");
        channelFuture.channel().closeFuture().sync();
    }
}
