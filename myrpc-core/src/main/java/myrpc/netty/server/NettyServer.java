package myrpc.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.exchange.model.MessageProtocol;
import myrpc.exchange.model.RpcRequest;
import myrpc.common.model.URLAddress;
import myrpc.exception.MyRpcRemotingException;
import myrpc.netty.message.codec.NettyDecoder;
import myrpc.netty.message.codec.NettyEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private static int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 16);

    private ServerBootstrap serverBootstrap;
    private final URLAddress urlAddress;

    public NettyServer(URLAddress urlAddress) {
        this.urlAddress = urlAddress;
    }

    public void init(){
        try {
            doOpen();
            doBind();
        }catch (Exception e){
            throw new MyRpcRemotingException("NettyServer init error",e);
        }
    }

    private void doOpen(){
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyServerBoss", true));
        EventLoopGroup workerGroup = new NioEventLoopGroup(DEFAULT_IO_THREADS,new DefaultThreadFactory("NettyServerWorker", true));

        NettyServerHandler nettyServerHandler = new NettyServerHandler();
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline()
                        // 编码、解码处理器
                        .addLast("encoder",new NettyEncoder<MessageProtocol<RpcRequest>>())
                        .addLast("decoder",new NettyDecoder())
                        // 心跳处理器
//                                .addLast("server-idle-handler",
//                                        new IdleStateHandler(0, 0, 5, MILLISECONDS))
                        // 实际调用业务方法的处理器 （单例）
                        .addLast("serverHandler",nettyServerHandler)
                    ;
                }
            });

        this.serverBootstrap = bootstrap;
    }

    private void doBind() throws InterruptedException {
        logger.info("server addr {} started on urlAddress {}", urlAddress.getHost(), urlAddress.getPort());
        ChannelFuture channelFuture = this.serverBootstrap.bind(urlAddress.getHost(), urlAddress.getPort()).sync();
        channelFuture.channel().closeFuture().sync();
    }
}
