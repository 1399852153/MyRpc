package myrpc.demo.rpc;

import myrpc.balance.SimpleRoundRobinBalance;
import myrpc.consumer.Consumer;
import myrpc.consumer.ConsumerBootstrap;
import myrpc.demo.common.model.User;
import myrpc.demo.common.service.UserService;
import myrpc.invoker.impl.FailoverInvoker;
import myrpc.registry.Registry;
import myrpc.registry.RegistryConfig;
import myrpc.registry.RegistryFactory;
import myrpc.registry.enums.RegistryCenterTypeEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

public class MyRpcConsumer {

    public static void main(String[] args) throws InterruptedException {
        Registry registry = RegistryFactory.getRegistry(
            new RegistryConfig(RegistryCenterTypeEnum.ZOOKEEPER_CURATOR.getCode(), "127.0.0.1:2181"));

        ConsumerBootstrap consumerBootstrap = new ConsumerBootstrap()
            .registry(registry)
            .loadBalance(new SimpleRoundRobinBalance());

        // 注册消费者
        Consumer<UserService> consumer = consumerBootstrap.registerConsumer(
            UserService.class,new FailoverInvoker(3));
        // 获得UserService的代理对象
        UserService userService = consumer.getProxy();

        User user = new User("Jerry",10);
        String message = "hello hello!";
        // 发起rpc调用并获得返回值
        User userFriend = userService.getUserFriend(user,message);
        System.out.println("userService.getUserFriend result=" + userFriend);

        try {
            userService.hasException("666");
        }catch (Exception e){
            System.out.println("userService.hasException!");
            e.printStackTrace();
        }

        {
            Thread.sleep(200L);
            Map<String,User> paramMap = new HashMap<>();
            paramMap.put("1",new User("a",10));
            paramMap.put("2",new User("b",10));
            paramMap.put("3",new User("c",10));

            List<User> response = userService.getFriends(paramMap);
            System.out.println("paramMap=" + paramMap + " response=" + response);
        }

        LockSupport.park();
    }
}
