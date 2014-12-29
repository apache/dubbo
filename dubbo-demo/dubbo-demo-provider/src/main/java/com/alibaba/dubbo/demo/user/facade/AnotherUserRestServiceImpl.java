/**
 * Copyright 1999-2014 dangdang.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.demo.user.facade;

import com.alibaba.dubbo.demo.user.User;
import com.alibaba.dubbo.demo.user.UserService;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * @author lishen
 */
public class AnotherUserRestServiceImpl implements AnotherUserRestService {

    private UserService userService;

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public User getUser(Long id) {
        System.out.println("Client name is " + RpcContext.getContext().getAttachment("clientName"));
        System.out.println("Client impl is " + RpcContext.getContext().getAttachment("clientImpl"));
        return userService.getUser(id);
    }

    public RegistrationResult registerUser(User user) {
        return new RegistrationResult(userService.registerUser(user));
    }
}
