package myrpc.demo.common.service;

import myrpc.demo.common.model.User;

public class UserServiceImpl implements UserService{
    @Override
    public User getUserFriend(User user, String message) {
        System.out.println("execute getUserFriend, user=" + user + ",message=" + message);

        // demo返回一个不同的user对象回去
        return new User(user.getName()+".friend", user.getAge()+1);
    }
}
