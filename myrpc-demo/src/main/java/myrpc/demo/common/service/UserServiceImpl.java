package myrpc.demo.common.service;

import myrpc.demo.common.model.User;
import myrpc.exception.MyRpcException;

public class UserServiceImpl implements UserService{
    @Override
    public User getUserFriend(User user, String message) {
        System.out.println("execute getUserFriend, user=" + user + ",message=" + message);

        // demo返回一个不同的user对象回去
        return new User(user.getName()+".friend", user.getAge()+1);
    }

    @Override
    public User hasException(String message) {
        throw new MyRpcException(message);
    }
}
