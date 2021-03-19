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
package org.apache.dubbo.config.spring.issues.issue6000;

import org.apache.dubbo.config.spring.EmbeddedZooKeeper;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.spring.issues.issue6000.adubbo.HelloDubbo;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * The test-case for https://github.com/apache/dubbo/issues/6000
 */
@Configuration
@EnableDubbo
@ComponentScan
@PropertySource("classpath:/META-INF/issues/issue6000/config.properties")
public class Issue6000Test {

    @Test
    public void test() throws Exception {
        EmbeddedZooKeeper zookeeper = new EmbeddedZooKeeper(2181, true);
        zookeeper.start();

        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Issue6000Test.class);

            HelloDubbo helloDubbo = context.getBean(HelloDubbo.class);
            String result = helloDubbo.sayHello("dubbo");
            System.out.println(result);

            context.close();
        } catch (Exception e){
            String s = e.toString();
            Assert.assertTrue(s.startsWith("org.apache.dubbo.rpc.RpcException: No provider available from registry 127.0.0.1:2181 for service org.apache.dubbo.config.spring.api.HelloService:1.0.0"));
        } finally {
            zookeeper.stop();
        }
    }

}
