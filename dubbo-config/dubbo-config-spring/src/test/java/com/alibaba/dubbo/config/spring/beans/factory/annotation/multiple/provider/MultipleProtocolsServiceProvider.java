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
package com.alibaba.dubbo.config.spring.beans.factory.annotation.multiple.provider;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;

/**
 * Multiple Protocols Service Provider
 */
@EnableDubbo
@PropertySource({
        "classpath:/META-INF/multiple-protocols-provider.properties",
        "classpath:/META-INF/dubbo-common.properties"
})
public class MultipleProtocolsServiceProvider {

    public static void main(String[] args) throws IOException {
//        EmbeddedZooKeeper embeddedZooKeeper = new EmbeddedZooKeeper(2181, false);
//        embeddedZooKeeper.start();

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(MultipleProtocolsServiceProvider.class);
        context.refresh();

        System.out.println("Enter any key to close the application");
        System.in.read();

        context.close();
//        embeddedZooKeeper.stop();
    }
}
