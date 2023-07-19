package myrpc.registry.enums;

import myrpc.exception.MyRpcException;

/**
 * 注册中心类型
 * @author shanreng
 */
public enum RegistryCenterTypeEnum {
    /**
     * 注册中心类型
     * */
    ZOOKEEPER("ZOOKEEPER","zookeeper注册中心(原始客户端)"),
    ZOOKEEPER_CURATOR("ZOOKEEPER_CURATOR","zookeeper注册中心(curator客户端)"),
    FAKE_REGISTRY("FAKE_REGISTRY","假的配置中心 无实际功能，用于点对点的rpc"),
    ;
    private final String code;
    private final String message;

    RegistryCenterTypeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static RegistryCenterTypeEnum getByCode(String code){
        for(RegistryCenterTypeEnum item : values()){
            if(item.code.equals(code)){
                return item;
            }
        }

        throw new MyRpcException("RegistryCenterTypeEnum getByCode not matched. code=" + code);
    }
}
