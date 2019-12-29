/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.dubbo.config.spring.context.annotation.consumer.test.component;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.api.DemoService;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@EnableDubbo
@PropertySource("classpath:dubbo-consumer-trueinit.properties")
@Component
public class TestDemoServiceComponentAnnotationWithoutTrueinit {
    private static final String remoteURL = "dubbo://127.0.0.1:12345?version=2.5.7";

    @Reference(version = "2.5.7", url = remoteURL)
    private DemoService demoService;

    public String sayHello(String name) {
        return demoService.sayName(name);
    }
}
