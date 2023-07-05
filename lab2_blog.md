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

## 5. 客户端请求设置超时时间
## 6. 总结