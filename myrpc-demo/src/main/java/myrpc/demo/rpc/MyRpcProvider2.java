package myrpc.demo.rpc;

import myrpc.common.model.URLAddress;
import myrpc.demo.common.service.UserService;
import myrpc.demo.common.service.UserServiceImpl;
import myrpc.netty.server.NettyServer;
import myrpc.provider.Provider;
import myrpc.registry.Registry;
import myrpc.registry.RegistryConfig;
import myrpc.registry.RegistryFactory;
import myrpc.registry.enums.RegistryCenterTypeEnum;

import java.net.InetAddress;

public class MyRpcProvider2 {

    public static void main(String[] args) throws Exception {
        String serverAddress = InetAddress.getLocalHost().getHostAddress();
        int port = 8082;

        URLAddress providerURLAddress = new URLAddress(serverAddress,port);

        Registry registry = RegistryFactory.getRegistry(
            new RegistryConfig(RegistryCenterTypeEnum.ZOOKEEPER.getCode(), "127.0.0.1:2181"));

        Provider<UserServiceImpl> provider = new Provider<>();
        provider.setInterfaceClass(UserService.class);
        provider.setRef(new UserServiceImpl());
        provider.setUrlAddress(providerURLAddress);
        provider.setRegistry(registry);

        provider.export();

        NettyServer nettyServer = new NettyServer(providerURLAddress);
        nettyServer.init();
    }

}
