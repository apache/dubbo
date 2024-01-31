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
package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class SignatureTest {

    @ParameterizedTest
    @ValueSource(
            classes = {
                com.alibaba.dubbo.config.ApplicationConfig.class,
                com.alibaba.dubbo.config.ArgumentConfig.class,
                com.alibaba.dubbo.config.ConsumerConfig.class,
                com.alibaba.dubbo.config.MethodConfig.class,
                com.alibaba.dubbo.config.ModuleConfig.class,
                com.alibaba.dubbo.config.MonitorConfig.class,
                com.alibaba.dubbo.config.ProtocolConfig.class,
                com.alibaba.dubbo.config.ProviderConfig.class,
                com.alibaba.dubbo.config.ReferenceConfig.class,
                com.alibaba.dubbo.config.RegistryConfig.class,
                com.alibaba.dubbo.config.ServiceConfig.class
            })
    void test(Class<?> targetClass) throws IOException {
        String[] lines = IOUtils.readLines(
                this.getClass().getClassLoader().getResourceAsStream("definition/" + targetClass.getName()));

        // only compare setter now.
        // getter cannot make it compatible with the old version.
        Set<String> setters = Arrays.stream(lines)
                .filter(StringUtils::isNotEmpty)
                .filter(s -> !s.startsWith("//"))
                .filter(s -> s.contains("set"))
                .collect(Collectors.toSet());

        for (Method method : targetClass.getMethods()) {
            setters.remove(
                    method.toString().replace(method.getDeclaringClass().getName() + ".", targetClass.getName() + "."));
        }

        assertThat(setters.toString(), setters, hasSize(0));
    }
}
