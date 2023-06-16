package myrpc.demo;

import myrpc.demo.common.model.User;
import myrpc.exchange.model.RpcResponse;
import myrpc.common.util.JsonUtil;
import myrpc.serialize.jdk.JdkSerializer;

public class T {

    public static void main(String[] args) {
        JdkSerializer jdkSerializer = new JdkSerializer();

        User user = new User("55",1);

        byte[] bytes = jdkSerializer.serialize(user);

        User user2 = (User) jdkSerializer.deserialize(bytes,user.getClass());
        System.out.println(user);
        System.out.println(user2);
    }
}
