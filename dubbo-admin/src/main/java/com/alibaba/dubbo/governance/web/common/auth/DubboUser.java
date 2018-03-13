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
package com.alibaba.dubbo.governance.web.common.auth;

import com.alibaba.dubbo.registry.common.domain.User;

import java.io.Serializable;

/**
 * MinasUser: DubboUser
 *
 */
public class DubboUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ThreadLocal<User> userHolder = new ThreadLocal<User>();

    private DubboUser() {
    }

    public static final User getCurrentUser() {
        return (User) userHolder.get();
    }

    public static final void setCurrentUser(User user) {
        userHolder.set(user);
    }

}
