# 自己动手实现rpc框架(一) 实现点对点的rpc通信
## 1. 什么是rpc?
**RPC**是远过程调用(Remote Procedure Call)的缩写形式，其区别于一个程序内部基本的过程调用(或者叫函数/方法调用)。
#####
随着应用程序变得越来越复杂，在单个机器上中仅通过一个进程来运行整个应用程序的方式已经难以满足现实中日益增长的需求。
开发者对应用程序进行模块化的拆分，以分布式部署的方式来降低程序整体的复杂度和提升性能方面的可拓展性(分而治之的思想)。
#####
拆分后部署在不同机器上的各个模块无法像之前那样通过内存寻址的方式来互相访问，而是需要通过网络来进行通信。
RPC最主要的功能就是在提供不同模块服务间的网络通信能力的同时，又尽可能的不丢失本地调用时语义的简洁性。rpc可以认为是分布式系统中类似人体经络一样的基础设施，因此有必要对其工作原理有一定的了解。
## 2. MyRpc介绍
要学习rpc的原理，理论上最好的办法就是去看流行的开源框架源码。但dubbo这样成熟的rpc框架由于已经迭代了很多年，为了满足多样的需求而有着复杂的架构和庞大的代码量。对于普通初学者往往很难从层层抽象封装中把握住关于rpc框架最核心的内容。
#####
MyRpc是我最近在学习MIT6.824分布式系统公开课时，使用java并基于netty实现的一个简易rpc框架，实现的过程中许多地方都参考了dubbo以及一些demo级别的rpc框架。  
由于MyRpc是demo级别的框架，理解起来会轻松不少。在对基础的rpc实现原理有一定了解后，对于后续研究dubbo等开源rpc框架会有一定的帮助。
#####
目前MyRpc实现了以下功能
1. 网络通信(netty做客户端、服务端网络交互，服务端使用一个线程池处理业务逻辑)
2. 实现消息的序列化（实现序列化方式的抽象，支持json、hessian、jdk序列化等）
3. 客户端代理生成(目前只实现了jdk动态代理)
4. 服务注册 + 注册中心集成(实现注册中心的抽象，但目前只支持用zookeeper做注册中心)
5. 集群负载均衡策略(实现负载均衡策略的抽象，支持roundRobin轮训，随机等)
6. 使用时间轮，支持设置消费者调用超时时间
#####
限于篇幅，以上功能会拆分为两篇博客分别介绍。其中前3个功能实现了基本的点对点通信的rpc功能，将在本篇博客中结合源码详细分析。

todo MyRpc架构图

## 3. MyRpc源码分析
### 3.1 基于netty的极简客户端/服务端交互demo
MyRpc是以netty为基础的，下面展示一个最基础的netty客户端/服务端交互的demo。
#####
netty服务端：
```java
/**
 * 最原始的netty服务端demo
 * */
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
```
#####
netty客户端：
```java
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
```
#####
* demo示例中，netty的服务端启动后绑定在本机127.0.0.1的8888端口上，等待来自客户端的连接。
* netty客户端向服务端发起连接请求，在成功建立连接后向服务端发送了一个User对象字符串对应的字节数组。
* 服务端在接受到这一字节数组后反序列化为User对象并打印在控制台，随后echo响应了一个字符串。客户端在接受到响应后，将echo字符串打印在了控制台上

### 3.2 设计MyRpc通信协议，解决黏包/拆包问题
上面展示了一个最基础的netty网络通信的demo，似乎一个点对点的传输功能已经得到了良好的实现。
但作为一个rpc框架，还需要解决tcp传输层基于字节流的消息黏包/拆包问题。
##### 黏包/拆包问题介绍
操作系统实现的传输层tcp协议中，向上层的应用保证尽最大可能的(best effort delivery)、可靠的传输字节流，但并不关心实际传输的数据包是否符合应用层的要求。  
#####
* **黏包问题：** 假设应用层发送的一次请求数据量比较小(比如0.1kb)，tcp层可能不会在接到应用请求后立即进行传输，而是会稍微等待一小会。
这样如果应用层在短时间内需要传输多次0.1kb的请求，就可以攒在一起批量传输，传输效率会高很多。
但这带来的问题就是接收端一次接受到的数据包内应用程序逻辑上的多次请求**黏连**在了一起，需要通过一些方法来将其拆分还原为一个个独立的信息给应用层。
* **拆包问题：** 假设应用层发送的一次请求数据量比较大(比如100Mb)，而tcp层的数据包容量的最大值是有限的，所以应用层较大的一次请求数据会被**拆分**为多个包分开发送。
这就导致接收端接受到的某个数据包其实并不是完整的应用层请求数据，没法直接交给应用程序去使用，
而必须等待后续对应请求的所有数据包都接受完成后，才能组装成完整的请求对象再交给应用层处理。
* 一个数据包中可能同时存在黏包问题和拆包问题(如下图所示)。

todo 黏包拆包示意图

##### 黏包/拆包问题解决方案
解决黏包/拆包问题最核心的思路是，如何知道一个应用层完整请求的边界。
对于黏包问题，基于边界可以独立的拆分出每一个请求；对于拆包问题，如果发现收到的数据包末尾没有边界，则继续等待新的数据包，直到发现边界后再一并上交给应用程序。  
#####
主流的解决黏包拆包的应用层消息协议有三种：
#####
|                    | 介绍                                                  | 优点                           | 缺点                                       |
|--------------------|-----------------------------------------------------|------------------------------|------------------------------------------|
| 1.基于固定长度的协议        | 每个消息都是固定的大小，如果实际上小于固定值，则需要填充                        | 简单;易于实现                      | 固定值过大，填充会浪费大量传输带宽；固定值过小则限制了可用的消息体大小      |
| 2.基于特殊分隔符的协议       | 约定一个特殊的分隔符，以这个分割符为消息边界                              | 简单;且消息体长度是可变的，性能好            | 消息体的业务数据不允许包含这个特殊分隔符，否则会错误的拆分数据包。因此兼容性较差 |
| 3.基于业务数据长度编码的协议    | 设计一个固定大小的消息请求头(比如固定16字节、20字节大小)，在消息请求头中包含实际的业务消息体长度 | 消息体长度可变，性能好；对业务数据内容无限制，兼容性也好 | 实现起来稍显复杂                                 |
#####
对于流行的rpc框架，一般都是选用性能与兼容性皆有的方案3：即自己设计一个固定大小的、包含了请求体长度字段的请求头。MyRpc参考dubbo，同样设计了一个固定16字节大小的请求头。
#####
请求头: MessageHeader
```java
/**
 * 共16字节的请求头
 * */
public class MessageHeader implements Serializable {

    public static final int MESSAGE_HEADER_LENGTH = 16;
    public static final int MESSAGE_SERIALIZE_TYPE_LENGTH = 5;
    public static final short MAGIC = (short)0x2233;

    // ================================ 消息头 =================================
    /**
     * 魔数(占2字节)
     * */
    private short magicNumber = MAGIC;

    /**
     * 消息标识(0代表请求事件；1代表响应事件， 占1位)
     * @see MessageFlagEnums
     * */
    private Boolean messageFlag;

    /**
     * 是否是双向请求(0代表oneWay请求；1代表twoWay请求）
     * （双向代表客户端会等待服务端的响应，单向则请求发送完成后即向上层返回成功)
     * */
    private Boolean twoWayFlag;

    /**
     * 是否是心跳消息(0代表正常消息；1代表心跳消息， 占1位)
     * */
    private Boolean eventFlag;

    /**
     * 消息体序列化类型(占5位，即所支持的序列化类型不得超过2的5次方，32种)
     * @see MessageSerializeType
     * */
    private Boolean[] serializeType;

    /**
     * 响应状态(占1字节)
     * */
    private byte responseStatus;

    /**
     * 消息的唯一id（占8字节）
     * */
    private long messageId;

    /**
     * 业务数据长度（占4字节）
     * */
    private int bizDataLength;
}
```
#####
完整的消息对象: MessageProtocol
```java
public class MessageProtocol<T> implements Serializable {
    /**
     * 请求头
     * */
    private MessageHeader messageHeader;

    /**
     * 请求体(实际的业务消息对象)
     * */
    private T bizDataBody;
}
```
##### 应用层rpc请求/响应对象
```java
/**
 * rpc请求对象
 * */
public class RpcRequest implements Serializable {

    private static final AtomicLong INVOKE_ID = new AtomicLong(0);

    /**
     * 消息的唯一id（占8字节）
     * */
    private final long messageId;

    /**
     * 接口名
     * */
    private String interfaceName;

    /**
     * 方法名
     * */
    private String methodName;

    /**
     * 参数类型数组(每个参数一项)
     * */
    private Class<?>[] parameterClasses;

    /**
     * 实际参数对象数组(每个参数一项)
     * */
    private Object[] params;

    public RpcRequest() {
        // 每个请求对象生成时都自动生成单机全局唯一的自增id
        this.messageId = INVOKE_ID.getAndIncrement();
    }
}
```
```java
/**
 * rpc响应对象
 * */
public class RpcResponse implements Serializable {

    /**
     * 消息的唯一id（占8字节）
     * */
    private long messageId;

    /**
     * 返回值
     */
    private Object returnValue;

    /**
     * 异常值
     */
    private Exception exceptionValue;
}
```
todo 附MyRpc消息示例图

##### 处理自定义消息的netty编解码器
在上一节的netty demo中的消息处理器中，一共做了两件事情；一是将原始数据包的字节流转化成了应用程序所需的String对象；二是拿到String对象后进行响应的业务处理(比如打印在控制台上)。
而netty框架允许配置多个消息处理器组成链条，按约定的顺序处理出站/入站的消息；因此从模块化的出发，应该将编码/解码的逻辑和实际业务的处理拆分成多个处理器。    
#####
在自定义的消息编码器、解码器中进行应用层请求/响应数据的序列化/反序列化，同时处理上述的黏包/拆包问题。
##### 
编解码工具类
```java
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
        MyRpcSerializer myRpcSerializer = MyRpcSerializerManager.getSerializer(messageHeader.getSerializeType());
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
        MyRpcSerializer myRpcSerializer = MyRpcSerializerManager.getSerializer(messageHeader.getSerializeType());
        return (T) myRpcSerializer.deserialize(bizDataBytes,messageBizDataType);
    }
}
```
#####
自定义编码器: NettyEncoder
```java
public class NettyEncoder<T> extends MessageToByteEncoder<MessageProtocol<T>> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MessageProtocol<T> messageProtocol, ByteBuf byteBuf) {
        // 继承自MessageToByteEncoder中，只需要将编码后的数据写入参数中指定的byteBuf中即可
        // MessageToByteEncoder源码逻辑中会自己去将byteBuf写入channel的
        MessageCodecUtil.messageEncode(messageProtocol,byteBuf);
    }
}
```
#####
自定义解码器: NettyDecoder
```java
/**
 * netty 解码器
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
            RpcRequest rpcRequest = MessageCodecUtil.messageBizDataDecode(messageHeader,byteBuf,RpcRequest.class);
            MessageProtocol<RpcRequest> messageProtocol = new MessageProtocol<>(messageHeader,rpcRequest);
            // 正确的解析完一个rpc请求消息
            return MessageDecodeResult.decodeSuccess(messageProtocol);
        }else{
            RpcResponse rpcResponse = MessageCodecUtil.messageBizDataDecode(messageHeader,byteBuf,RpcResponse.class);
            MessageProtocol<RpcResponse> messageProtocol = new MessageProtocol<>(messageHeader,rpcResponse);
            // 正确的解析完一个rpc响应消息
            return MessageDecodeResult.decodeSuccess(messageProtocol);
        }
    }
}
```
#####

##### 解决了黏包/拆包问题后的demo示例
demo的服务示例:
```java
public class User implements Serializable {

    private String name;
    private Integer age;
}
```
```java
public interface UserService {

    User getUserFriend(User user, String message);
}
```
```java
public class UserServiceImpl implements UserService {
    @Override
    public User getUserFriend(User user, String message) {
        System.out.println("execute getUserFriend, user=" + user + ",message=" + message);

        // demo返回一个不同的user对象回去
        return new User(user.getName() + ".friend", user.getAge() + 1);
    }
}
```
#####
netty服务端：
```java
public class RpcServer {

    private static final Map<String,Object> interfaceImplMap = new HashMap<>();

    static{
        /**
         * 简单一点配置死实现
         * */
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
        messageHeader.setSerializeType(rpcRequestMessageProtocol.getMessageHeader().getSerializeType());

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
```
#####
netty客户端：
```java
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
        String message = "hello hello!";
        rpcRequest.setParams(new Object[]{user,message});

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
```

todo 附netty处理流程图

### 3.3 基于动态代理实现一个完整的点对点rpc功能
截止目前，我们已经实现了一个点对点rpc客户端/服务端交互的功能，但是客户端这边的逻辑依然比较复杂(buildMessage方法)。  
前面提到，rpc中很重要的功能就是保持本地调用时语义的简洁性，即客户端实际使用时是希望直接用以下这种方式来进行调用，而不是去繁琐的构建底层的消息。
```java
    User user = new User("Jerry",10);
    String message = "hello hello!";
    // 发起rpc调用并获得返回值
    User userFriend = userService.getUserFriend(user,message);
    System.out.println("userService.getUserFriend result=" + userFriend);
```
#####
rpc框架需要屏蔽掉构造底层消息发送/接受，序列化/反序列化相关的复杂性，而这时候就需要引入代理模式(动态代理)了。  
在MyRpc的底层，我们将客户端需要调用的一个服务(比如UserService)抽象为Consumer对象，服务端的一个具体服务实现抽象为Provider对象。
其中包含了对应的服务的类以及对应的服务地址，客户端这边使用jdk的动态代理生成代理对象，将复杂的、需要屏蔽的消息处理/网络交互等逻辑都封装在这个代理对象中。
#####
```java
public class Consumer<T> {

    private Class<?> interfaceClass;
    private T proxy;

    private Bootstrap bootstrap;
    private URLAddress urlAddress;

    public Consumer(Class<?> interfaceClass, Bootstrap bootstrap, URLAddress urlAddress) {
        this.interfaceClass = interfaceClass;
        this.bootstrap = bootstrap;
        this.urlAddress = urlAddress;

        ClientDynamicProxy clientDynamicProxy = new ClientDynamicProxy(bootstrap,urlAddress);

        this.proxy = (T) Proxy.newProxyInstance(
            clientDynamicProxy.getClass().getClassLoader(),
            new Class[]{interfaceClass},
            clientDynamicProxy);
    }

    public T getProxy() {
        return proxy;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }
}
```
```java
public class ConsumerBootstrap {

    private final Map<Class<?>,Consumer<?>> consumerMap = new HashMap<>();
    private final Bootstrap bootstrap;
    private final URLAddress urlAddress;

    public ConsumerBootstrap(Bootstrap bootstrap, URLAddress urlAddress) {
        this.bootstrap = bootstrap;
        this.urlAddress = urlAddress;
    }

    public <T> Consumer<T> registerConsumer(Class<T> clazz){
        if(!consumerMap.containsKey(clazz)){
            Consumer<T> consumer = new Consumer<>(clazz,this.bootstrap,this.urlAddress);
            consumerMap.put(clazz,consumer);
            return consumer;
        }

        throw new MyRpcException("duplicate consumer! clazz=" + clazz);
    }
}
```
```java
public class Provider<T> {

    private Class<?> interfaceClass;
    private T ref;
    private URLAddress urlAddress;
}
```
##### 客户端代理对象生成
```java
/**
 * 客户端动态代理
 * */
public class ClientDynamicProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientDynamicProxy.class);

    private final Bootstrap bootstrap;
    private final URLAddress urlAddress;

    public ClientDynamicProxy(Bootstrap bootstrap, URLAddress urlAddress) {
        this.bootstrap = bootstrap;
        this.urlAddress = urlAddress;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object localMethodResult = processLocalMethod(proxy,method,args);
        if(localMethodResult != null){
            // 处理toString等对象自带方法，不发起rpc调用
            return localMethodResult;
        }

        logger.debug("ClientDynamicProxy before: methodName=" + method.getName());

        // 构造请求和协议头
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setInterfaceName(method.getDeclaringClass().getName());
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParameterClasses(method.getParameterTypes());
        rpcRequest.setParams(args);

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setMessageFlag(MessageFlagEnums.REQUEST.getCode());
        messageHeader.setTwoWayFlag(false);
        messageHeader.setEventFlag(true);
        messageHeader.setSerializeType(GlobalConfig.messageSerializeType.getCode());
        messageHeader.setResponseStatus((byte)'a');
        messageHeader.setMessageId(rpcRequest.getMessageId());

        logger.debug("ClientDynamicProxy rpcRequest={}", JsonUtil.obj2Str(rpcRequest));

        ChannelFuture channelFuture = bootstrap.connect(urlAddress.getHost(),urlAddress.getPort()).sync();
        Channel channel = channelFuture.sync().channel();
        // 通过Promise，将netty的异步转为同步,参考dubbo DefaultFuture
        DefaultFuture<RpcResponse> defaultFuture = DefaultFutureManager.createNewFuture(channel,rpcRequest);

        channel.writeAndFlush(new MessageProtocol<>(messageHeader,rpcRequest));
        logger.debug("ClientDynamicProxy writeAndFlush success, wait result");

        // 调用方阻塞在这里
        RpcResponse rpcResponse = defaultFuture.get();

        logger.debug("ClientDynamicProxy defaultFuture.get() rpcResponse={}",rpcResponse);

        return processRpcResponse(rpcResponse);
    }

    private Object processLocalMethod(Object proxy, Method method, Object[] args) throws Exception {
        // 处理toString等对象自带方法，不发起rpc调用
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(proxy, args);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return proxy.toString();
            } else if ("hashCode".equals(methodName)) {
                return proxy.hashCode();
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return proxy.equals(args[0]);
        }

        // 返回null标识非本地方法，需要进行rpc调用
        return null;
    }

    private Object processRpcResponse(RpcResponse rpcResponse){
        if(rpcResponse.getExceptionValue() == null){
            // 没有异常，return正常的返回值
            return rpcResponse.getReturnValue();
        }else{
            // 有异常，往外抛出去
            throw new MyRpcRemotingException(rpcResponse.getExceptionValue());
        }
    }
}
```
##### 代理模式下点对点rpc的客户端demo
```java
public class RpcClientProxy {

    public static void main(String[] args) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(8, new DefaultThreadFactory("NettyClientWorker", true));

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

        ConsumerBootstrap consumerBootstrap = new ConsumerBootstrap(bootstrap, new URLAddress("127.0.0.1", 8888));
        Consumer<UserService> userServiceConsumer = consumerBootstrap.registerConsumer(UserService.class);

        // 获得UserService的代理对象
        UserService userService = userServiceConsumer.getProxy();

        User user = new User("Jerry", 10);
        String message = "hello hello!";
        // 发起rpc调用并获得返回值
        User userFriend = userService.getUserFriend(user, message);
        System.out.println("userService.getUserFriend result=" + userFriend);
    }
}
```
可以看到，引入了代理模式后的使用方式就变得简单很多了。   
到这一步，我们已经实现了一个点对点的rpc通信的能力，并且如博客开头中所提到的，没有丧失本地调用语义的简洁性。
## 总结