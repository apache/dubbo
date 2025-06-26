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
package org.apache.dubbo.spring.security.oauth2.jackson;

import org.apache.dubbo.spring.security.jackson.ObjectMapperCodec;
import org.apache.dubbo.spring.security.jackson.ObjectMapperCodecCustomer;
import org.apache.dubbo.spring.security.oauth2.OAuth2SecurityModule;

import java.util.List;

import com.fasterxml.jackson.databind.Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;

public class OAuth2ObjectMapperCodecCustomer implements ObjectMapperCodecCustomer {

    @Override
    public void customize(ObjectMapperCodec objectMapperCodec) {
        objectMapperCodec.configureMapper(mapper -> {
            mapper.registerModule(new OAuth2SecurityModule());
            List<Module> securityModules =
                    SecurityJackson2Modules.getModules(this.getClass().getClassLoader());
            mapper.registerModules(securityModules);
        });
    }
}
