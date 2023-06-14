package myrpc.provider;

import myrpc.common.model.URLAddress;

public class Provider<T> {

    private Class<?> interfaceClass;
    private T ref;
    private URLAddress urlAddress;

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

}
