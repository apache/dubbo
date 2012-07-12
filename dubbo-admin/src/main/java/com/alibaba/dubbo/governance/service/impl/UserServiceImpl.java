/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-4-15
 * $Id: UserServiceImpl.java 182013 2012-06-26 10:32:43Z tony.chenl $
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
package com.alibaba.dubbo.governance.service.impl;

import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.governance.service.UserService;
import com.alibaba.dubbo.registry.common.domain.User;

/**
 * IBatisUserService
 * 
 * @author william.liangf
 */
public class UserServiceImpl extends AbstractService implements UserService {

    public List<User> findAllUsers() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, User> findAllUsersMap() {
        // TODO Auto-generated method stub
        return null;
    }

    public User findUser(String username) {
        // TODO Auto-generated method stub
        return null;
    }

    public User findById(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    public void createUser(User user) {
        // TODO Auto-generated method stub
        
    }

    public void updateUser(User user) {
        // TODO Auto-generated method stub
        
    }

    public void modifyUser(User user) {
        // TODO Auto-generated method stub
        
    }

    public boolean updatePassword(User user, String oldPassword) {
        // TODO Auto-generated method stub
        return false;
    }

    public void resetPassword(User user) {
        // TODO Auto-generated method stub
        
    }

    public void enableUser(User user) {
        // TODO Auto-generated method stub
        
    }

    public void disableUser(User user) {
        // TODO Auto-generated method stub
        
    }

    public void deleteUser(User user) {
        // TODO Auto-generated method stub
        
    }

    public List<User> findUsersByServiceName(String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

}
