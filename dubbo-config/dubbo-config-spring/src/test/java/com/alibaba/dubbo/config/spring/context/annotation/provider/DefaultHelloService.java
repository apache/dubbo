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
package com.alibaba.dubbo.config.spring.context.annotation.provider;

import com.alibaba.dubbo.config.spring.api.HelloService;
import org.springframework.stereotype.Service;

/**
 * Default {@link HelloService} annotation with Spring's {@link Service}
 * and Dubbo's {@link com.alibaba.dubbo.config.annotation.Service}
 *
 * @since TODO
 */
@Service
@com.alibaba.dubbo.config.annotation.Service
public class DefaultHelloService implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Greeting, " + name;
    }

}
