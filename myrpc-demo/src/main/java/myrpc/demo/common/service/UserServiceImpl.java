package myrpc.demo.common.service;

import myrpc.demo.common.model.User;
import myrpc.exception.MyRpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserServiceImpl implements UserService{
    @Override
    public User getUserFriend(User user, String message) {
        System.out.println("execute getUserFriend, user=" + user + ",message=" + message);

        // demo返回一个不同的user对象回去
        return new User(user.getName()+".friend", user.getAge()+1);
    }

    @Override
    public User hasException(String message) {
        throwException(message);

        return null;
    }

    @Override
    public List<User> getFriends(Map<String, User> userMap) {
        return new ArrayList<>(userMap.values());
    }

    private void throwException(String message) throws MyRpcException{
        throwException2(message);
    }

    private void throwException2(String message) throws MyRpcException{
        throw new MyRpcException(message);
    }
}
