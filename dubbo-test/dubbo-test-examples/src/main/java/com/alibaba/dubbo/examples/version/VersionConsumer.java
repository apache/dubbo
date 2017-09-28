/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.examples.version;

import com.alibaba.dubbo.examples.version.api.VersionService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * VersionConsumer
 *
 * @author william.liangf
 */
public class VersionConsumer {

    public static void main(String[] args) throws Exception {
        String config = VersionConsumer.class.getPackage().getName().replace('.', '/') + "/version-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        VersionService versionService = (VersionService) context.getBean("versionService");
        for (int i = 0; i < 10000; i++) {
            String hello = versionService.sayHello("world");
            System.out.println(hello);
            Thread.sleep(2000);
        }
        System.in.read();
    }

}
