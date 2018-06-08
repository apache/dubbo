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
package com.alibaba.dubbo.examples.rest.impl;

import com.alibaba.dubbo.examples.rest.api.User;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service("userService")
public class UserServiceImpl implements com.alibaba.dubbo.examples.rest.api.UserService {

    private final AtomicLong idGen = new AtomicLong();

    @Override
    public User getUser(Long id) {
        return new User(id, "username" + id);
    }


    @Override
    public Long registerUser(User user) {
//        System.out.println("Username is " + user.getName());
        return idGen.incrementAndGet();
    }
}
