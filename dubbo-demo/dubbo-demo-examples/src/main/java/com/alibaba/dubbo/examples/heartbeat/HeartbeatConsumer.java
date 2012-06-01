/*
 * Copyright 1999-2012 Alibaba Group.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.examples.heartbeat;

import com.alibaba.dubbo.examples.heartbeat.api.HelloService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class HeartbeatConsumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                HeartbeatConsumer.class.getPackage().getName().replace('.', '/') + "/heartbeat-consumer.xml");
        context.start();
        HelloService hello = (HelloService) context.getBean("helloService");
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            System.out.println(hello.sayHello("kimi-" + i));
            Thread.sleep(10000);
        }
    }

}
