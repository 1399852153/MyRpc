package myrpc.provider;

import myrpc.exception.MyRpcRemotingException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderManager {

    public static Map<String,Provider> providerMapCache = new ConcurrentHashMap<>();

    public static Provider getProvider(String name) {
        Provider provider = providerMapCache.get(name);

        if(provider == null){
            throw new MyRpcRemotingException("can not find provider name=" + name);
        }

        return provider;
    }

    public static void putProvider(String name, Provider provider) {
        providerMapCache.put(name, provider);
    }
}
