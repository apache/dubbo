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

import org.apache.dubbo.config.builders.ApplicationBuilder;
import org.apache.dubbo.config.builders.MetadataReportBuilder;
import org.apache.dubbo.config.builders.ProtocolBuilder;
import org.apache.dubbo.config.builders.RegistryBuilder;
import org.apache.dubbo.config.builders.ServiceBuilder;

import java.io.IOException;

/**
 * Dubbo Provider Bootstrap
 *
 * @since 2.7.4
 */
public class DubboServiceProviderBootstrap {

    public static void main(String[] args) throws IOException {

        new DubboBootstrap()
                .application(ApplicationBuilder.newBuilder().name("dubbo-provider-demo").metadata("remote").build())
                .metadataReport(MetadataReportBuilder.newBuilder().address("zookeeper://127.0.0.1:2181").build())
//                .application(ApplicationBuilder.newBuilder().name("dubbo-provider-demo").build())
                .registry(RegistryBuilder.newBuilder().address("zookeeper://127.0.0.1:2181?registry-type=service").build())
                .protocol(ProtocolBuilder.newBuilder().port(-1).name("dubbo").build())
                .service(ServiceBuilder.newBuilder().id("test").interfaceClass(EchoService.class).ref(new EchoServiceImpl()).build())
                .start()
                .await();
    }
}
