package myrpc.demo.common.service;

import myrpc.demo.common.model.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    User getUserFriend(User user, String message);

    User hasException(String message);

    List<User> getFriends(Map<String,User> userMap);
}
