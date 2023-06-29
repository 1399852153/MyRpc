package myrpc.netty.client;

import myrpc.common.model.URLAddress;

import java.util.concurrent.ConcurrentHashMap;

public class NettyClientFactory {

    private static final ConcurrentHashMap<URLAddress,NettyClient> nettyClientCache = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();

    public static NettyClient getNettyClient(URLAddress urlAddress){
        NettyClient nettyClient = nettyClientCache.get(urlAddress);
        if(nettyClient != null){
            return nettyClient;
        }else{
            synchronized (LOCK){
                if(nettyClientCache.get(urlAddress) != null){
                    return nettyClientCache.get(urlAddress);
                }

                // 双重检查
                NettyClient newNettyClient = new NettyClient(urlAddress);
                newNettyClient.init();
                nettyClientCache.put(urlAddress,newNettyClient);
                return newNettyClient;
            }
        }
    }
}
