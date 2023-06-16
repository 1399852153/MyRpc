package myrpc.demo.common.service;

import myrpc.demo.common.model.User;

public interface UserService {

    User getUserFriend(User user, String message);

    User hasException(String message);
}
