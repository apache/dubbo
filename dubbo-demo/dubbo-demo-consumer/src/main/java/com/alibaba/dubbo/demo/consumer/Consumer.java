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
package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

    public static void main(String[] args) {
        //Prevent to get IPV6 address,this way only work in debug mode
        //But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
        context.start();
        DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy

        while (true) {
            try {
//                try {
//                    demoService.sayHello(gen(1024000));
//                } catch (Exception e) {
//                }

//                String hello = demoService.sayHello("world"); // call remote method
//                System.out.println(hello); // get result


//                demoService.say01(null);
//                demoService.say01("TestException");
//                demoService.hello("01");
//                ((EchoService) demoService).$echo("test4u");
//                ((EchoService) demoService).$echo("test4u");

                demoService.sayHello("world");
//                ProtocolConfig.destroyAll();

                Thread.sleep(10000000);
//                demoService.say02();
//                demoService.say03();
//                demoService.say04();

                // 参数回调
                // https://dubbo.gitbooks.io/dubbo-user-book/demos/callback-parameter.html
//                demoService.callbackParam("shuaiqi", new ParamCallback() {
//                    @Override
//                    public void doSome(Cat msg) {
//                        System.out.println("回调biubiu：" + msg);
//                    }
//                });

//                demoService.bye(new Cat().setName("小猫"));
//                demoService.bye(new Dog().setAge(10));

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }

    private static String gen(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append("s");
        }
        return sb.toString();
    }
}
