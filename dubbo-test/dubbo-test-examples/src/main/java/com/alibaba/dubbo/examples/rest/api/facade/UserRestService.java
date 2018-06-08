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
package com.alibaba.dubbo.examples.rest.api.facade;

import com.alibaba.dubbo.examples.rest.api.User;

import javax.validation.constraints.Min;

/**
 * This interface acts as some kind of service broker for the original UserService
 * <p>
 * Here we want to simulate the twitter/weibo rest api, e.g.
 * <p>
 * http://localhost:8888/user/1.json
 * http://localhost:8888/user/1.xml
 */
public interface UserRestService {

    /**
     * the request object is just used to test jax-rs injection.
     */
    User getUser(@Min(value = 1L, message = "User ID must be greater than 1") Long id/*, HttpServletRequest request*/);

    RegistrationResult registerUser(User user);
}
