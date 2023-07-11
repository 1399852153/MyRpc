package myrpc.registry;

import myrpc.common.model.ServiceInfo;
import myrpc.common.util.JsonUtil;
import myrpc.exception.MyRpcException;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
            logger.debug("createServiceNameNode success! serviceNameNodePath={}",serviceNameNodePath);
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
            logger.debug("createProviderInfoNode success! path={}",providerInfoNodePath);
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
            logger.debug("ZookeeperListener process! path={}",path);

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
