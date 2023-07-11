package myrpc.registry;

import myrpc.common.model.ServiceInfo;
import myrpc.common.util.JsonUtil;
import myrpc.exception.MyRpcException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

            logger.debug("createServiceNameNode success! serviceNameNodePath={}", serviceNameNodePath);
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
            logger.debug("createProviderInfoNode success! path={}", providerInfoNodePath);
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

            logger.debug("findProviderInfoList={}",JsonUtil.obj2Str(serviceInfoList));
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
            logger.debug("ZookeeperListener process! serviceName={}",serviceName);

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
