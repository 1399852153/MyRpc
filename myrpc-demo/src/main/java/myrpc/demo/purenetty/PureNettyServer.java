package myrpc.demo.purenetty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.demo.common.model.User;
import myrpc.serialize.json.JsonUtil;

public class PureNettyServer {

    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        EventLoopGroup workerGroup = new NioEventLoopGroup(8,new DefaultThreadFactory("NettyServerWorker", true));

        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline()
                        // 实际调用业务方法的处理器
                        .addLast("serverHandler", new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf requestByteBuf) {
                                String requestStr = requestByteBuf.toString(CharsetUtil.UTF_8);
                                System.out.println("PureNettyServer read request=" + JsonUtil.json2Obj(requestStr, User.class));

                                // 服务端响应echo
                                ByteBuf byteBuf = Unpooled.copiedBuffer("echo:" + requestStr,CharsetUtil.UTF_8);
                                channelHandlerContext.writeAndFlush(byteBuf);
                            }
                        })
                    ;
                }
            });

        ChannelFuture channelFuture = bootstrap.bind("127.0.0.1", 8888).sync();

        System.out.println("netty server started!");
        // 一直阻塞在这里
        channelFuture.channel().closeFuture().sync();
    }
}
