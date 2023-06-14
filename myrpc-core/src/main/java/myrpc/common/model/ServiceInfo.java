package myrpc.common.model;

public class ServiceInfo {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务地址
     * */
    private URLAddress urlAddress;

    public ServiceInfo(String serviceName, URLAddress urlAddress) {
        this.serviceName = serviceName;
        this.urlAddress = urlAddress;
    }

    public ServiceInfo() {
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public URLAddress getUrlAddress() {
        return urlAddress;
    }

    public void setUrlAddress(URLAddress urlAddress) {
        this.urlAddress = urlAddress;
    }
}
