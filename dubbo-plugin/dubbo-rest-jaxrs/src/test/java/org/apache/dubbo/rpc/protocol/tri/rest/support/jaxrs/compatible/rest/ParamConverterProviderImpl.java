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
package org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.compatible.rest;

import org.apache.dubbo.rpc.protocol.tri.rest.support.jaxrs.compatible.User;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ParamConverterProviderImpl implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type type, Annotation[] annotations) {
        if (rawType.isAssignableFrom(User.class)) {
            return (ParamConverter<T>) new UserParamConverter();
        }
        return null;
    }

    static class UserParamConverter implements ParamConverter<User> {
        @Override
        public User fromString(String param) {
            User user = new User();
            user.setId(1L);
            user.setAge(20);
            return user;
        }

        @Override
        public String toString(User user) {
            return "User{" + "id=" + user.getId() + ", name='" + user.getName() + '\'' + ", age=" + user.getAge() + '}';
        }
    }
}
