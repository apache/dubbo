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

package org.apache.dubbo.spring.security.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.security.jackson2.CoreJackson2Module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ObjectMapperCodec {

    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectMapperCodec() {
        registerDefaultModule();
    }

    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try {

            if (bytes == null || bytes.length == 0) {
                return null;
            }

            return mapper.readValue(bytes, clazz);

        } catch (Exception exception) {
            throw new RuntimeException(
                String.format("objectMapper! deserialize error %s", exception));
        }
    }

    public <T> T deserialize(String content, Class<T> clazz) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        return deserialize(content.getBytes(), clazz);
    }

    public String serialize(Object object) {
        try {

            if (object == null) {
                return null;
            }

            return mapper.writeValueAsString(object);

        } catch (Exception ex) {
            throw new RuntimeException(String.format("objectMapper! serialize error %s", ex));
        }
    }

    public ObjectMapperCodec addModule(SimpleModule simpleModule) {
        mapper.registerModule(simpleModule);
        return this;
    }

    public ObjectMapperCodec configureMapper(Consumer<ObjectMapper> objectMapperConfigure) {
        objectMapperConfigure.accept(this.mapper);
        return this;
    }

    private void registerDefaultModule() {
        mapper.registerModule(new CoreJackson2Module());
        mapper.registerModule(new JavaTimeModule());

        List<String> jacksonModuleClassNameList = new ArrayList<>();
        jacksonModuleClassNameList.add("org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module");
        jacksonModuleClassNameList.add("org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module");
        jacksonModuleClassNameList.add("org.springframework.security.web.server.jackson2.WebServerJackson2Module");
        jacksonModuleClassNameList.add("com.fasterxml.jackson.module.paramnames.ParameterNamesModule");
        jacksonModuleClassNameList.add("org.springframework.security.web.jackson2.WebServletJackson2Module");
        jacksonModuleClassNameList.add("org.springframework.security.web.jackson2.WebJackson2Module");
        jacksonModuleClassNameList.add("org.springframework.boot.jackson.JsonMixinModule");
        jacksonModuleClassNameList.add("org.springframework.security.ldap.jackson2.LdapJackson2Module");
        loadModuleIfPresent(jacksonModuleClassNameList);

    }

    private void loadModuleIfPresent(List<String> jacksonModuleClassNameList) {
        for (String moduleClassName : jacksonModuleClassNameList) {
            try {
                SimpleModule objectMapperModule = (SimpleModule) ClassUtils.forName(moduleClassName,
                    ObjectMapperCodec.class.getClassLoader()).getDeclaredConstructor().newInstance();
                mapper.registerModule(objectMapperModule);

            } catch (Throwable ex) {
            }
        }
    }

}
