package myrpc.provider;

import myrpc.common.model.ServiceInfo;
import myrpc.common.model.URLAddress;
import myrpc.registry.Registry;

public class Provider<T> {

    private Class<?> interfaceClass;
    private T ref;
    private URLAddress urlAddress;
    private Registry registry;

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    public URLAddress getUrlAddress() {
        return urlAddress;
    }

    public void setUrlAddress(URLAddress urlAddress) {
        this.urlAddress = urlAddress;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void export(){
        // 放入本地的provider緩存中
        ProviderManager.putProvider(this.interfaceClass.getName(),this);

        // 注冊到注冊中心
        this.registry.doRegistry(
            new ServiceInfo(this.interfaceClass.getName(),this.urlAddress));
    }
}
