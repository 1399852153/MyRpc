package myrpc.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.common.model.URLAddress;
import myrpc.exception.MyRpcRemotingException;
import myrpc.netty.message.codec.NettyDecoder;
import myrpc.netty.message.codec.NettyEncoder;

public class NettyClient {

    private static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    private final URLAddress urlAddress;
    private Bootstrap bootstrap;
    private volatile Channel channel;

    /**
     * 简单起见，EventLoopGroup设置成全局复用的(多个Netty客户端使用同一个事件循环组节约资源)
     * */
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(DEFAULT_IO_THREADS,
        new DefaultThreadFactory("NettyClientWorker", true));

    public NettyClient(URLAddress urlAddress) {
        this.urlAddress = urlAddress;
    }

    public void init() {
        try {
            doOpen();
            doConnect();
        }catch (Exception e){
            throw new MyRpcRemotingException("NettyClient init error",e);
        }
    }

    public Channel getChannel(){
        return this.channel;
    }

    public void send(Object message) throws InterruptedException {
        if(!channel.isActive() || !channel.isOpen()){
            // 简单处理下连接失效，尝试重新创建新的连接(比如对端进程被杀，连接失效等)
            doConnect();
        }

        // 很多case没考虑到，可以参考dubbo的NettyChannel.send方法
        ChannelFuture channelFuture = channel.writeAndFlush(message);
        channelFuture.sync();

        Throwable cause = channelFuture.cause();
        if (cause != null) {
            throw new MyRpcRemotingException("NettyClient send error",cause);
        }
    }

    private void doOpen(){
        if(this.bootstrap == null) {
            Bootstrap bootstrap = new Bootstrap();

            bootstrap.group(this.eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    // 编码、解码处理器
                                    .addLast("decoder", new NettyDecoder())
                                    // 心跳处理器
//                                .addLast("server-idle-handler",
//                                        new IdleStateHandler(0, 0, 5, MILLISECONDS))
                                    .addLast("encoder", new NettyEncoder<>())
                                    // 实际调用业务方法的处理器
                                    .addLast("clientHandler", new NettyRpcResponseHandler())
                            ;
                        }
                    });

            this.bootstrap = bootstrap;
        }
    }

    private void doConnect() throws InterruptedException {
        ChannelFuture future = bootstrap.connect(urlAddress.getHost(), urlAddress.getPort());

        // 写的很简单，异常case都没考虑（可以参考NettyClient.doConnect实现）
        this.channel = future.sync().channel();
    }

    @Override
    public String toString() {
        return "NettyClient{" +
                "urlAddress=" + urlAddress +
                ", channel=" + channel +
                '}';
    }
}
