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
package org.apache.dubbo.springboot.demo.consumer;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.springboot.demo.DemoService2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DubboService(parameters = {"security","mTLS,sa_jwt"})
public class DemoServiceImpl2 implements DemoService2 {

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl2.class);

    @Override
    public String sayHello(String name) {

        logger.info("Hello " + name + ", request from consumer: "
                + RpcContext.getContext().getRemoteAddress());
        return "hello," + name;
    }
}
