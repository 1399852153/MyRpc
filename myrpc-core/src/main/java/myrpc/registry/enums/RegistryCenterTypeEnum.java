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
    ZOOKEEPER("ZOOKEEPER","zookeeper注册中心"),
    ;
    private String code;
    private String message;

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
