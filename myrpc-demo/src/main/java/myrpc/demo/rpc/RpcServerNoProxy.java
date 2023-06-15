package myrpc.demo.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import myrpc.common.enums.MessageFlagEnums;
import myrpc.common.enums.MessageSerializeType;
import myrpc.common.model.MessageHeader;
import myrpc.common.model.MessageProtocol;
import myrpc.common.model.RpcRequest;
import myrpc.common.model.RpcResponse;
import myrpc.demo.common.model.User;
import myrpc.demo.common.service.UserService;
import myrpc.demo.common.service.UserServiceImpl;
import myrpc.netty.message.codec.NettyDecoder;
import myrpc.netty.message.codec.NettyEncoder;
import myrpc.netty.server.NettyServerHandler;
import myrpc.serialize.json.JsonUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class RpcServerNoProxy {

    private static Map<String,Object> interfaceImplMap = new HashMap<>();

    static{
        interfaceImplMap.put(UserService.class.getName(), new UserServiceImpl());
    }

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
                        // 编码、解码处理器
                        .addLast("encoder", new NettyEncoder<>())
                        .addLast("decoder", new NettyDecoder())
                        // 实际调用业务方法的处理器
                        .addLast("serverHandler", new SimpleChannelInboundHandler<MessageProtocol<RpcRequest>>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol<RpcRequest> msg) {
                                // 找到本地的方法进行调用，并获得返回值(demo，简单起见直接同步调用)
                                MessageProtocol<RpcResponse> result = handlerRpcRequest(msg);

                                // 将返回值响应给客户端
                                ctx.writeAndFlush(result);
                            }
                        });
                }
            });

        ChannelFuture channelFuture = bootstrap.bind("127.0.0.1", 8888).sync();

        System.out.println("netty server started!");
        // 一直阻塞在这里
        channelFuture.channel().closeFuture().sync();
    }

    private static MessageProtocol<RpcResponse> handlerRpcRequest(MessageProtocol<RpcRequest> rpcRequestMessageProtocol){
        long requestMessageId = rpcRequestMessageProtocol.getMessageHeader().getMessageId();

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageId(requestMessageId);
        messageHeader.setMessageFlag(MessageFlagEnums.RESPONSE.getCode());
        messageHeader.setTwoWayFlag(false);
        messageHeader.setEventFlag(false);
        messageHeader.setSerializeType(MessageSerializeType.JSON.getCode());

        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessageId(requestMessageId);

        try {
            // 反射调用具体的实现方法
            Object result = invokeTargetService(rpcRequestMessageProtocol.getBizDataBody());

            // 设置返回值
            rpcResponse.setReturnValue(result);
        }catch (Exception e){
            // 调用具体实现类时，出现异常，设置异常的值
            rpcResponse.setExceptionValue(e);
        }

        return new MessageProtocol<>(messageHeader,rpcResponse);
    }

    private static Object invokeTargetService(RpcRequest rpcRequest) throws Exception {
        String interfaceName = rpcRequest.getInterfaceName();
        Object serviceImpl = interfaceImplMap.get(interfaceName);

        // 按照请求里的方法名和参数列表找到对应的方法
        final Method method = serviceImpl.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterClasses());

        // 传递参数，反射调用该方法并返回结果
        return method.invoke(serviceImpl, rpcRequest.getParams());
    }
}
