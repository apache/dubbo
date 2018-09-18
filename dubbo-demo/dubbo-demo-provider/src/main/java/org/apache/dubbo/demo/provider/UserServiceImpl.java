package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.User;
import org.apache.dubbo.demo.UserService;

/**
 * @author cvictory ON 2018/9/18
 */
public class UserServiceImpl implements UserService {
    @Override
    public Integer getCount(User user) {
        return user.hashCode();
    }
}
