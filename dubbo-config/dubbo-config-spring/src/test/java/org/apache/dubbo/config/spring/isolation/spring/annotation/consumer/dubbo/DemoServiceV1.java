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
package org.apache.dubbo.config.spring.isolation.spring.annotation.consumer.dubbo;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.api.Box;
import org.apache.dubbo.config.spring.api.DemoService;
import org.springframework.stereotype.Component;

@Component("dubbo-demoServiceV1")
public class DemoServiceV1 implements DemoService {
    @DubboReference(version = "1.0.0", group = "Group1", scope = "remote", protocol = "dubbo")
    private DemoService demoService;

    @Override
    public String sayName(String name) {
        return demoService.sayName(name);
    }

    @Override
    public Box getBox() {
        return null;
    }
}
