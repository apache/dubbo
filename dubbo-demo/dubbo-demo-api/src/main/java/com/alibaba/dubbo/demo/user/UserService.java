package com.alibaba.dubbo.demo.user;

import javax.validation.constraints.Min;

/**
 * @author morly
 */
public interface UserService {
    User getUser(@Min(value=1L, message="User ID must be greater than 1") Long id);

    Long registerUser(User user);
}
