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
package org.apache.dubbo.bootstrap;

import java.io.IOException;

import static org.apache.dubbo.bootstrap.EchoService.GROUP;
import static org.apache.dubbo.bootstrap.EchoService.VERSION;

/**
 * Dubbo Provider Bootstrap
 *
 * @since 2.7.3
 */
public class DubboServiceProviderBootstrap {

    public static void main(String[] args) throws IOException {

        new DubboBootstrap()
                .application("dubbo-provider-demo")
                .next()
                .registry()
                .address("nacos://127.0.0.1:8848?registry-type=service")
                .next()
                .protocol()
                .name("dubbo")
                .port(-1)
                .next()
                .service("test")
                .interfaceClass(EchoService.class)
                .ref(new EchoServiceImpl())
                .group(GROUP)
                .version(VERSION)
                .next()
                .start()
                .await();
    }
}
