/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-4-15
 * $Id: UserService.java 182013 2012-06-26 10:32:43Z tony.chenl $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service;

import java.util.List;

import com.alibaba.dubbo.registry.common.domain.User;

/**
 * UserService
 * 
 * @author william.liangf
 */
public interface UserService {

    List<User> findAllUsers();
    
    User findUser(String username);
    
    User findById(Long id);

    void createUser(User user);
    
    void updateUser(User user);
    
    void modifyUser(User user);
    
    boolean updatePassword(User user, String oldPassword);
    
    void resetPassword(User user);

    void enableUser(User user);
    
    void disableUser(User user);

    void deleteUser(User user);

}
