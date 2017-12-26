/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.governance.service.UserService;
import com.alibaba.dubbo.registry.common.domain.User;
import com.alibaba.dubbo.registry.common.util.Coder;

import java.util.List;
import java.util.Map;

/**
 * IBatisUserService
 *
 */
public class UserServiceImpl extends AbstractService implements UserService {

    private String rootPassword;
    private String guestPassword;

    public void setRootPassword(String password) {
        this.rootPassword = (password == null ? "" : password);
    }

    public void setGuestPassword(String password) {
        this.guestPassword = (password == null ? "" : password);
    }

    public User findUser(String username) {
        if ("guest".equals(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(Coder.encodeMd5(username + ":" + User.REALM + ":" + guestPassword));
            user.setName(username);
            user.setRole(User.GUEST);
            user.setEnabled(true);
            user.setLocale("zh");
            user.setServicePrivilege("");
            return user;
        } else if ("root".equals(username)) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(Coder.encodeMd5(username + ":" + User.REALM + ":" + rootPassword));
            user.setName(username);
            user.setRole(User.ROOT);
            user.setEnabled(true);
            user.setLocale("zh");
            user.setServicePrivilege("*");
            return user;
        }
        return null;
    }

    public List<User> findAllUsers() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, User> findAllUsersMap() {
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
