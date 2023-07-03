package myrpc.consumer.context;

import myrpc.common.model.URLAddress;

public class ConsumerRpcContext {

    /**
     * 指定请求需要调用的服务方地址(主要给MIT6.824实验用)
     * */
    private URLAddress targetProviderAddress;

    public URLAddress getTargetProviderAddress() {
        return targetProviderAddress;
    }

    public void setTargetProviderAddress(URLAddress targetProviderAddress) {
        this.targetProviderAddress = targetProviderAddress;
    }
}
