package myrpc.registry;

import myrpc.registry.enums.RegistryCenterTypeEnum;

public class RegistryConfig {

    /**
     * 注册中心类型
     * @see RegistryCenterTypeEnum
     * */
    private final String registryCenterType;

    /**
     * 注册中心地址
     * */
    private final String registryAddress;

    public RegistryConfig(String registryCenterType, String registryAddress) {
        this.registryCenterType = registryCenterType;
        this.registryAddress = registryAddress;
    }

    public String getRegistryCenterType() {
        return registryCenterType;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }
}
