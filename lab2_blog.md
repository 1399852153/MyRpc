# 自己动手实现rpc框架(二) 实现集群间rpc通信
## 1. 集群间rpc通信
上一篇博客中MyRpc框架实现了基本的点对点rpc通信功能。而在这篇博客中我们需要实现MyRpc的集群间rpc通信功能。  
* [自己动手实现rpc框架(一) 实现点对点的rpc通信](https://www.cnblogs.com/xiaoxiongcanguan/p/17506728.html)
#####
上篇博客的点对点rpc通信实现中，客户端和服务端的ip地址和端口都是固定配置死的。
而通常为了提升服务总负载，客户端和服务端都是以集群的方式部署的(水平拓展)，客户端和服务端的节点都不止1个。  
集群条件下出现了很多新的问题需要解决：
1. 对于某一特定服务，客户端该如何知道当前环境下哪些机器能提供这一服务? 
2. 服务端集群中的某些节点如果发生了变化(比如老节点下线或宕机)，客户端该如何及时的感知到，而不会调用到已经停止服务的节点上？
3. 存在多个服务端时，客户端应该向哪一个服务端节点发起请求？怎样才能使得每个服务端的负载尽量均衡，而不会让某些服务端饥饿或者压力过大。
## 2. 服务注册/发现与注册中心
* 针对第一个问题，最先想到的自然是直接在每个客户端都配置一个固定的服务端节点列表，但这一方案无法很好的解决服务端节点动态变化的问题。
如果一个服务端节点下线了，就需要人工的去修改每个客户端那里维护的服务端节点列表的话，在集群节点数量较多、服务端节点上下线频繁的场景下是不可接受的。
* 解决这一问题的思路是服务端节点信息的中心化，将服务端节点的信息都集中维护在一个地方。  
服务端在启动成功后将自己的信息注册在上面(**服务注册**)，而客户端也能实时的查询出最新的服务端列表(**服务发现**)。  
这个统一维护服务端节点信息的地方被叫做**注册中心**，一般是以独立服务的形式与rpc的服务端/客户端机器部署在同一环境内。
* 由于节点信息的中心化，所以注册中心需要具备高可用能力(集群部署来提供容错能力)，避免单点故障而导致整个rpc集群的不可用。
同时在服务端节点因为一些原因不可用时能实时的感知并移除掉对应节点，同时通知监听变更客户端(解决第二个关于provider信息实时性的问题)。  
因此zookeeper、eureka、nacos、etcd等等具备上述能力的中间件都被广泛的用作rpc框架的注册中心。
##### MyRpc集成注册中心
MyRpc目前支持使用zookeeper作为注册中心。  
zookeeper作为一个高性能的分布式协调器，存储的数据以ZNode节点树的形式存在。ZNode节点有两种属性，有序/无序，持久/临时。  
* rpc框架中一般设置一个持久的根路径节点用于与zk上存储其它的业务数据作区分(例如/my_rpc)。
* 在根节点下有着代表着某一特定服务的子节点，其也是持久节点。服务子节点的路径名是标识接口的唯一名称(比如包名+类名：myrpc.demo.common.service.UserService)
* 而服务节点下则可以存储各种关于provider、consumer等等相关的元数据。  
  MyRpc中为了简单起见，服务节点的子节点直接就是对应特定provider注册的临时节点。临时节点中数据保存了provider的ip/port等必要的信息。
  由于是临时节点，在provider因为各种故障而不可用而导致与zookeeper的连接断开，zookeeper会在等待一小会后将该临时节点删除，并通知监听该服务的客户端以刷新客户端的对应配置。
#####
todo 附MyRpc的zookeeper节点结构图
#####
注册中心接口
```java
/**
 * 注册中心的抽象
 * */
public interface Registry {

    /**
     * 服务注册
     * */
    void doRegistry(ServiceInfo serviceInfo);

    /**
     * 服务发现
     * */
    List<ServiceInfo> discovery(String serviceName);
}
```
zookeeper注册中心实现(原始的zk客户端)
```java
/**
 * 简易的zk注册中心(原始的zk客户端很多地方都需要用户去处理异常，但为了更简单的展示zk注册中心的使用，基本上没有处理这些异常情况)
 * */
public class ZookeeperRegistry implements Registry{

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final ZooKeeper zooKeeper;

    private final ConcurrentHashMap<String,List<ServiceInfo>> serviceInfoCacheMap = new ConcurrentHashMap<>();

    public ZookeeperRegistry(String zkServerAddress) {
        try {
            this.zooKeeper = new ZooKeeper(zkServerAddress,2000, event -> {});

            // 确保root节点是一定存在的
            createPersistentNode(MyRpcRegistryConstants.BASE_PATH);
        } catch (Exception e) {
            throw new MyRpcException("init zkClient error",e);
        }
    }

    @Override
    public void doRegistry(ServiceInfo serviceInfo) {
        // 先创建永久的服务名节点
        createServiceNameNode(serviceInfo.getServiceName());
        // 再创建临时的providerInfo节点
        createProviderInfoNode(serviceInfo);
    }

    @Override
    public List<ServiceInfo> discovery(String serviceName) {
        return serviceInfoCacheMap.computeIfAbsent(serviceName,(key)-> findProviderInfoList(serviceName));
    }

    private String getServiceNameNodePath(String serviceName){
        return MyRpcRegistryConstants.BASE_PATH + "/" + serviceName;
    }

    // ================================ zk工具方法 ==================================
    private void createServiceNameNode(String serviceName){
        try {
            String serviceNameNodePath = getServiceNameNodePath(serviceName);

            // 服务名节点是永久节点
            createPersistentNode(serviceNameNodePath);
            logger.info("createServiceNameNode success! serviceNameNodePath={}",serviceNameNodePath);
        } catch (Exception e) {
            throw new MyRpcException("createServiceNameNode error",e);
        }
    }

    private void createProviderInfoNode(ServiceInfo serviceInfo){
        try {
            String serviceNameNodePath = getServiceNameNodePath(serviceInfo.getServiceName());
            // 子节点用一个uuid做path防重复
            String providerInfoNodePath = serviceNameNodePath + "/" + UUID.randomUUID();

            String providerInfoJsonStr = JsonUtil.obj2Str(serviceInfo);
            // providerInfo节点是临时节点(如果节点宕机了，zk的连接断开一段时间后，临时节点会被自动删除)
            zooKeeper.create(providerInfoNodePath, providerInfoJsonStr.getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            logger.info("createProviderInfoNode success! path={}",providerInfoNodePath);
        } catch (Exception e) {
            throw new MyRpcException("createProviderInfoNode error",e);
        }
    }

    private void createPersistentNode(String path){
        try {
            if (zooKeeper.exists(path, false) == null) {
                // 服务名节点是永久节点
                zooKeeper.create(path, "".getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }catch (Exception e){
            throw new MyRpcException("createPersistentNode error",e);
        }
    }

    private List<ServiceInfo> findProviderInfoList(String serviceName){
        String serviceNameNodePath = getServiceNameNodePath(serviceName);

        List<ServiceInfo> serviceInfoList = new ArrayList<>();
        try {
            List<String> providerInfoPathList = zooKeeper.getChildren(serviceNameNodePath, new ZookeeperListener(serviceNameNodePath));
            for(String providerInfoPath : providerInfoPathList){
                try{
                    String fullProviderInfoPath = serviceNameNodePath + "/" + providerInfoPath;
                    byte[] data = zooKeeper.getData(fullProviderInfoPath,false,null);
                    String jsonStr = new String(data,StandardCharsets.UTF_8);
                    ServiceInfo serviceInfo = JsonUtil.json2Obj(jsonStr,ServiceInfo.class);

                    serviceInfoList.add(serviceInfo);
                }catch (Exception e){
                    logger.error("findProviderInfoList getData error",e);
                }
            }

            logger.info("findProviderInfoList={}",JsonUtil.obj2Str(serviceInfoList));
            return serviceInfoList;
        } catch (Exception e) {
            throw new MyRpcException("findProviderInfoList error",e);
        }
    }

    private class ZookeeperListener implements Watcher{
        private final String path;
        private final String serviceName;

        public ZookeeperListener(String serviceName) {
            this.path = getServiceNameNodePath(serviceName);
            this.serviceName = serviceName;
        }

        @Override
        public void process(WatchedEvent event) {
            logger.info("ZookeeperListener process! path={}",path);

            try {
                // 刷新缓存
                List<ServiceInfo> serviceInfoList = findProviderInfoList(path);
                serviceInfoCacheMap.put(serviceName,serviceInfoList);
            } catch (Exception e) {
                logger.error("ZookeeperListener getChildren error! path={}",path,e);
            }
        }
    }
}
```
zookeeper注册中心实现(curator客户端，通过自动重试等操作解决了原生客户端的一些坑)
```java
public class ZkCuratorRegistry implements Registry {
    private static final Logger logger = LoggerFactory.getLogger(ZkCuratorRegistry.class);

    private CuratorFramework curatorZkClient;

    private final ConcurrentHashMap<String, List<ServiceInfo>> serviceInfoCacheMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, PathChildrenCache> nodeCacheMap = new ConcurrentHashMap<>();


    public ZkCuratorRegistry(String zkServerAddress) {
        try {
            this.curatorZkClient = CuratorFrameworkFactory.newClient(zkServerAddress, new ExponentialBackoffRetry(3000, 1));
            this.curatorZkClient.start();
        } catch (Exception e) {
            throw new MyRpcException("init zkClient error", e);
        }
    }

    @Override
    public void doRegistry(ServiceInfo serviceInfo) {
        // 先创建永久的服务名节点
        createServiceNameNode(serviceInfo.getServiceName());
        // 再创建临时的providerInfo节点
        createProviderInfoNode(serviceInfo);
    }

    @Override
    public List<ServiceInfo> discovery(String serviceName) {
        return this.serviceInfoCacheMap.computeIfAbsent(serviceName,(key)->{
            List<ServiceInfo> serviceInfoList = findProviderInfoList(serviceName);

            // 创建对子节点的监听
            String serviceNodePath = getServiceNameNodePath(serviceName);
            PathChildrenCache pathChildrenCache = new PathChildrenCache(curatorZkClient, serviceNodePath, true);
            try {
                pathChildrenCache.start();
                nodeCacheMap.put(serviceName,pathChildrenCache);
                pathChildrenCache.getListenable().addListener(new ZkCuratorListener(serviceName));
            } catch (Exception e) {
                throw new MyRpcException("PathChildrenCache start error!",e);
            }

            return serviceInfoList;
        });
    }

    private void createServiceNameNode(String serviceName) {
        try {
            String serviceNameNodePath = getServiceNameNodePath(serviceName);

            // 服务名节点是永久节点
            if (curatorZkClient.checkExists().forPath(serviceNameNodePath) == null) {
                curatorZkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(serviceNameNodePath);
            }

            logger.info("createServiceNameNode success! serviceNameNodePath={}", serviceNameNodePath);
        } catch (Exception e) {
            throw new MyRpcException("createServiceNameNode error", e);
        }
    }

    private void createProviderInfoNode(ServiceInfo serviceInfo) {
        try {
            String serviceNameNodePath = getServiceNameNodePath(serviceInfo.getServiceName());
            // 子节点用一个uuid做path防重复
            String providerInfoNodePath = serviceNameNodePath + "/" + UUID.randomUUID();

            String providerInfoJsonStr = JsonUtil.obj2Str(serviceInfo);

            // providerInfo节点是临时节点(如果节点宕机了，zk的连接断开一段时间后，临时节点会被自动删除)
            curatorZkClient.create()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(providerInfoNodePath, providerInfoJsonStr.getBytes(StandardCharsets.UTF_8));
            logger.info("createProviderInfoNode success! path={}", providerInfoNodePath);
        } catch (Exception e) {
            throw new MyRpcException("createProviderInfoNode error", e);
        }
    }

    private String getServiceNameNodePath(String serviceName) {
        return MyRpcRegistryConstants.BASE_PATH + "/" + serviceName;
    }

    private List<ServiceInfo> findProviderInfoList(String serviceName) {
        String serviceNameNodePath = getServiceNameNodePath(serviceName);

        try {
            List<String> providerInfoPathList = curatorZkClient.getChildren().forPath(serviceNameNodePath);
            List<ServiceInfo> serviceInfoList = new ArrayList<>();

            for(String providerInfoPath : providerInfoPathList){
                try{
                    String fullProviderInfoPath = serviceNameNodePath + "/" + providerInfoPath;
                    byte[] data = curatorZkClient.getData().forPath(fullProviderInfoPath);
                    String jsonStr = new String(data,StandardCharsets.UTF_8);
                    ServiceInfo serviceInfo = JsonUtil.json2Obj(jsonStr,ServiceInfo.class);

                    serviceInfoList.add(serviceInfo);
                }catch (Exception e){
                    logger.error("findProviderInfoList getData error",e);
                }
            }

            logger.info("findProviderInfoList={}",JsonUtil.obj2Str(serviceInfoList));
            return serviceInfoList;
        } catch (Exception e) {
            throw new MyRpcException("findProviderInfoList error",e);
        }
    }

    private class ZkCuratorListener implements PathChildrenCacheListener {
        private final String serviceName;

        public ZkCuratorListener(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
            logger.info("ZookeeperListener process! serviceName={}",serviceName);

            try {
                // 刷新缓存
                List<ServiceInfo> serviceInfoList = findProviderInfoList(serviceName);
                serviceInfoCacheMap.put(serviceName,serviceInfoList);
            } catch (Exception e) {
                logger.error("ZookeeperListener getChildren error! serviceName={}",serviceName,e);
            }
        }
    }
}
```
## 3. 负载均衡
现在客户端已经能通过服务发现得到实时的provider集合了，那么客户端发起请求时应该如何决定向哪个provider发起请求以实现provider侧的负载均衡呢？  
* 常见的负载均衡算法有很多，MyRpc抽象出了负载均衡算法的接口，并实现了最简单的两种负载均衡算法(无权重的纯随机 + roundRobin)。
* 在实际的环境中，每个provider可能机器的配置、网络延迟、运行时的动态负载、请求处理的延迟等都各有不同，优秀的负载均衡算法能够通过预先的配置和采集运行时的各项指标来计算出最优的请求顺序。
MyRpc实现的负载均衡算法在这里只起到一个抛砖引玉的参考作用。
#####
负载均衡接口
```java
/**
 * 负载均衡选择器
 * */
public interface LoadBalance {

    ServiceInfo select(List<ServiceInfo> serviceInfoList);
}
```
#####
随机负载均衡
```java
/**
 * 无权重，纯随机的负载均衡选择器
 * */
public class RandomLoadBalance implements LoadBalance{
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfoList) {
        int selectedIndex = ThreadLocalRandom.current().nextInt(serviceInfoList.size());
        return serviceInfoList.get(selectedIndex);
    }
}
```
#####
```java
/**
 * 无权重的轮训负载均衡（后续增加带权重的轮训）
 * */
public class SimpleRoundRobinBalance implements LoadBalance{

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfoList) {
        if(serviceInfoList.isEmpty()){
            throw new MyRpcException("serviceInfoList is empty!");
        }

        // 考虑一下溢出，取绝对值
        int selectedIndex = Math.abs(count.getAndIncrement());
        return serviceInfoList.get(selectedIndex % serviceInfoList.size());
    }
}
```
## 4. 支持多种集群服务调用方式
由于需要通过网络发起rpc调用，比起本地调用很容易因为网络波动、远端机器故障等原因而导致调用失败。  
客户端有时希望能通过重试等方式屏蔽掉可能出现的偶发错误，尽可能的保证rpc请求的成功率，最好rpc框架能解决这个问题。
另一方面，能够安全重试的基础是下游服务能够做到幂等，否则重复的请求会带来意想不到的后果，而不幂等的下游服务只能至多调用一次。  
因此rpc框架需要能允许用户不同的服务可以有不同的集群服务调用方式，这样幂等的服务可以配置成可自动重试N次的failover调用，或者只能调用1次的fast-fail调用等。
#####
MyRpc的Invoker接口用于抽象上述的不同集群调用方式，并简单的实现了failover和fast-fail等多种调用方式（参考dubbo）。
```java
public interface InvokerCallable {
    RpcResponse invoke(NettyClient nettyClient);
}
```
```java
/**
 * 不同的集群调用方式
 * */
public interface Invoker {

    RpcResponse invoke(InvokerCallable callable, String serviceName,
                                      Registry registry, LoadBalance loadBalance);
}
```
```java
/**
 * 快速失败，无论成功与否调用1次就返回
 * */
public class FastFailInvoker implements Invoker {

  private static final Logger logger = LoggerFactory.getLogger(FastFailInvoker.class);

  @Override
  public RpcResponse invoke(InvokerCallable callable, String serviceName,
                            Registry registry, LoadBalance loadBalance) {
    List<ServiceInfo> serviceInfoList = registry.discovery(serviceName);
    logger.debug("serviceInfoList.size={},serviceInfoList={}",serviceInfoList.size(), JsonUtil.obj2Str(serviceInfoList));
    NettyClient nettyClient = InvokerUtil.getTargetClient(serviceInfoList,loadBalance);
    logger.info("ClientDynamicProxy getTargetClient={}", nettyClient);

    // fast-fail，简单的调用一次就行，有错误就直接向上抛
    return callable.invoke(nettyClient);
  }
}
```
```java
/**
 * 故障转移调用(如果调用出现了错误，则重试指定次数)
 * 1 如果重试过程中成功了，则快读返回
 * 2 如果重试了指定次数后还是没成功，则抛出异常
 * */
public class FailoverInvoker implements Invoker {

    private static final Logger logger = LoggerFactory.getLogger(FailoverInvoker.class);

    private final int defaultRetryCount = 2;
    private final int retryCount;

    public FailoverInvoker() {
        this.retryCount = defaultRetryCount;
    }

    public FailoverInvoker(int retryCount) {
        this.retryCount = Math.max(retryCount,1);
    }

    @Override
    public RpcResponse invoke(InvokerCallable callable, String serviceName, Registry registry, LoadBalance loadBalance) {
        MyRpcException myRpcException = null;

        for(int i=0; i<retryCount; i++){
            List<ServiceInfo> serviceInfoList = registry.discovery(serviceName);
            logger.debug("serviceInfoList.size={},serviceInfoList={}",serviceInfoList.size(), JsonUtil.obj2Str(serviceInfoList));
            NettyClient nettyClient = InvokerUtil.getTargetClient(serviceInfoList,loadBalance);
            logger.info("ClientDynamicProxy getTargetClient={}", nettyClient);

            try {
                RpcResponse rpcResponse = callable.invoke(nettyClient);
                if(myRpcException != null){
                    // 虽然最终重试成功了，但是之前请求失败过
                    logger.warn("FailRetryInvoker finally success, but there have been failed providers");
                }
                return rpcResponse;
            }catch (Exception e){
                myRpcException = new MyRpcException(e);

                logger.warn("FailRetryInvoker callable.invoke error",e);
            }
        }

        // 走到这里说明经过了retryCount次重试依然不成功，myRpcException一定不为null
        throw myRpcException;
    }
}
```
##### 接入invoker之后的客户端请求逻辑
```java
/**
 * 客户端动态代理
 * */
public class ClientDynamicProxy implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ClientDynamicProxy.class);

    private final Registry registry;
    private final LoadBalance loadBalance;
    private final Invoker invoker;

    public ClientDynamicProxy(Registry registry, LoadBalance loadBalance, Invoker invoker) {
        this.registry = registry;
        this.loadBalance = loadBalance;
        this.invoker = invoker;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Tuple<Object,Boolean> localMethodResult = processLocalMethod(proxy,method,args);
        if(localMethodResult.getRight()){
            // right为true,代表是本地方法，返回toString等对象自带方法的执行结果，不发起rpc调用
            return localMethodResult.getLeft();
        }

        logger.debug("ClientDynamicProxy before: methodName=" + method.getName());

        String serviceName = method.getDeclaringClass().getName();

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

        RpcResponse rpcResponse = this.invoker.invoke((nettyClient)->{
            Channel channel = nettyClient.getChannel();
            // 将netty的异步转为同步,参考dubbo DefaultFuture
            DefaultFuture<RpcResponse> newDefaultFuture = DefaultFutureManager.createNewFuture(channel,rpcRequest);

            try {
                nettyClient.send(new MessageProtocol<>(messageHeader,rpcRequest));

                // 调用方阻塞在这里
                return newDefaultFuture.get();
            } catch (Exception e) {
                throw new MyRpcException("InvokerCallable error!",e);
            }
        },serviceName,registry,loadBalance);

        logger.debug("ClientDynamicProxy defaultFuture.get() rpcResponse={}",rpcResponse);

        return processRpcResponse(rpcResponse);
    }

    /**
     * 处理本地方法
     * @return tuple.right 标识是否是本地方法， true是
     * */
    private Tuple<Object,Boolean> processLocalMethod(Object proxy, Method method, Object[] args) throws Exception {
        // 处理toString等对象自带方法，不发起rpc调用
        if (method.getDeclaringClass() == Object.class) {
            return new Tuple<>(method.invoke(proxy, args),true);
        }
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            if ("toString".equals(methodName)) {
                return new Tuple<>(proxy.toString(),true);
            } else if ("hashCode".equals(methodName)) {
                return new Tuple<>(proxy.hashCode(),true);
            }
        } else if (parameterTypes.length == 1 && "equals".equals(methodName)) {
            return new Tuple<>(proxy.equals(args[0]),true);
        }

        // 返回null标识非本地方法，需要进行rpc调用
        return new Tuple<>(null,false);
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
## 5. 客户端请求设置超时时间
客户端发起请求后，可能由于网络原因，可能由于服务端负载过大等原因而迟迟无法收到回复。
出于性能或者自身业务的考虑，客户端不能无限制的等待下去，因此rpc框架需要能允许客户端设置请求的超时时间。
在一定的时间内如果无法收到响应则需要抛出超时异常，令调用者及时的感知到问题。
#####
在客户端侧DefaultFuture.get方法，指定超时时间是可以做到这一点的。
但其依赖底层操作系统的定时任务机制，超时时间的精度很高(nanos级别)，但在高并发场景下性能不如时间轮。  
具体原理可以参考我之前的博客：[时间轮TimeWheel工作原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/17128575.html)
#####
MyRpc参考dubbo，引入时间轮来实现客户端设置请求超时时间的功能。
```java
public class DefaultFutureManager {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFutureManager.class);

    public static final Map<Long,DefaultFuture> DEFAULT_FUTURE_CACHE = new ConcurrentHashMap<>();
    public static final HashedWheelTimer TIMER = new HashedWheelTimer();

    public static void received(RpcResponse rpcResponse){
        Long messageId = rpcResponse.getMessageId();

        logger.debug("received rpcResponse={},DEFAULT_FUTURE_CACHE={}",rpcResponse,DEFAULT_FUTURE_CACHE);
        DefaultFuture defaultFuture = DEFAULT_FUTURE_CACHE.remove(messageId);

        if(defaultFuture != null){
            logger.debug("remove defaultFuture success");
            if(rpcResponse.getExceptionValue() != null){
                // 异常处理
                defaultFuture.completeExceptionally(rpcResponse.getExceptionValue());
            }else{
                // 正常返回
                defaultFuture.complete(rpcResponse);
            }
        }else{
            // 可能超时了，接到响应前已经remove掉了这个future(超时和实际接到请求都会调用received方法)
            logger.debug("remove defaultFuture fail");
        }
    }

    public static DefaultFuture createNewFuture(Channel channel, RpcRequest rpcRequest){
        DefaultFuture defaultFuture = new DefaultFuture(channel,rpcRequest);
        // 增加超时处理的逻辑
        newTimeoutCheck(defaultFuture);

        return defaultFuture;
    }

    public static DefaultFuture getFuture(long messageId){
        return DEFAULT_FUTURE_CACHE.get(messageId);
    }

    /**
     * 增加请求超时的检查任务
     * */
    public static void newTimeoutCheck(DefaultFuture defaultFuture){
        TimeoutCheckTask timeoutCheckTask = new TimeoutCheckTask(defaultFuture.getMessageId());
        TIMER.newTimeout(timeoutCheckTask, defaultFuture.getTimeout(), TimeUnit.MILLISECONDS);
    }
}
```
```java
public class TimeoutCheckTask implements TimerTask {

    private final long messageId;

    public TimeoutCheckTask(long messageId) {
        this.messageId = messageId;
    }

    @Override
    public void run(Timeout timeout) {
        DefaultFuture defaultFuture = DefaultFutureManager.getFuture(this.messageId);
        if(defaultFuture == null || defaultFuture.isDone()){
            // 请求已经在超时前返回，处理过了,直接返回即可
            return;
        }

        // 构造超时的响应
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setMessageId(this.messageId);
        rpcResponse.setExceptionValue(new MyRpcTimeoutException(
            "request timeout：" + defaultFuture.getTimeout() + " channel=" + defaultFuture.getChannel()));

        DefaultFutureManager.received(rpcResponse);
    }
}
```
## 6. 总结
* 经过两个lib的迭代，目前MyRpc已经是一个麻雀虽小五脏俱全的rpc框架了。
  虽然无论在功能上还是在各种细节的处理上都还有很多需要优化的地方，但作为一个demo级别的框架，其没有过多的抽象封装，更有利于rpc框架的初学者去理解。  
* 博客中展示的完整代码在我的github上：https://github.com/1399852153/MyRpc (release/lab2分支)，内容如有错误，还请多多指教。