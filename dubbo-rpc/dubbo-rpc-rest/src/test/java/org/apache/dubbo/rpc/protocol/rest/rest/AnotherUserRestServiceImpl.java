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
package org.apache.dubbo.rpc.protocol.rest.rest;

import org.apache.dubbo.rpc.protocol.rest.User;

import java.util.Map;


public class AnotherUserRestServiceImpl implements AnotherUserRestService {


    @Override
    public User getUser(Long id) {

        User user = new User();
        user.setId(id);
        return user;
    }

    @Override
    public RegistrationResult registerUser(User user) {
        return new RegistrationResult(user.getId());
    }

    @Override
    public String getContext() {

        return "context";
    }

    @Override
    public byte[] bytes(byte[] bytes) {
        return bytes;
    }

    @Override
    public Long number(Long number) {
        return number;
    }

    @Override
    public String headerMap(Map<String, String> headers) {
        return headers.get("headers");
    }


}
