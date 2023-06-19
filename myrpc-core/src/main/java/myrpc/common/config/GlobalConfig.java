package myrpc.common.config;

import myrpc.common.enums.MessageSerializeType;

public class GlobalConfig {

    /**
     * 简单起见，直接写死(应该是可配置的)
     * */
    public static MessageSerializeType messageSerializeType = MessageSerializeType.HESSIAN;
}
