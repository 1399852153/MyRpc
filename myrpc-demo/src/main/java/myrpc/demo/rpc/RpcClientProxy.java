package myrpc.demo.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.common.model.URLAddress;
import myrpc.consumer.Consumer;
import myrpc.consumer.ConsumerBootstrap;
import myrpc.demo.common.model.User;
import myrpc.demo.common.service.UserService;
import myrpc.netty.client.NettyRpcResponseHandler;
import myrpc.netty.message.codec.NettyDecoder;
import myrpc.netty.message.codec.NettyEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RpcClientProxy {

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

                        // 响应处理器
                        .addLast("clientHandler", new NettyRpcResponseHandler())
                    ;
                }
            });

        ConsumerBootstrap consumerBootstrap = new ConsumerBootstrap(bootstrap,new URLAddress("127.0.0.1",8888));
        Consumer<UserService> userServiceConsumer = consumerBootstrap.registerConsumer(UserService.class);

        // 获得UserService的代理对象
        UserService userService = userServiceConsumer.getProxy();

        User user = new User("Jerry",10);
        String message = "hello hello!";
        // 发起rpc调用并获得返回值
        User userFriend = userService.getUserFriend(user,message);
        System.out.println("userService.getUserFriend result=" + userFriend);

        try {
            userService.hasException("666");
        }catch (Exception e){
            System.out.println("userService.hasException!");
            e.printStackTrace();
        }

        {
            Thread.sleep(200L);
            Map<String,User> paramMap = new HashMap<>();
            paramMap.put("1",new User("a",10));
            paramMap.put("2",new User("b",10));
            paramMap.put("3",new User("c",10));

            List<User> response = userService.getFriends(paramMap);
            System.out.println("paramMap=" + paramMap + " response=" + response);
        }
    }
}
