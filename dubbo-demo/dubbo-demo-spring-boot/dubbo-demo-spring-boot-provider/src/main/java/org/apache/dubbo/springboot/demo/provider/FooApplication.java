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
package org.apache.dubbo.springboot.demo.provider;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.springboot.demo.DemoService2;
import org.apache.dubbo.xds.istio.IstioConstant;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableDubbo(scanBasePackages = {"org.apache.dubbo.springboot.demo.provider"})
public class FooApplication {

    //    @DubboReference(cluster = "xds", providedBy = "httpbin")
    @DubboReference(lazy = true,parameters = {"security","mTLS,sa_jwt"})
    private DemoService2 demoService;

    public static void main(String[] args) throws Exception {
        System.setProperty(IstioConstant.WORKLOAD_NAMESPACE_KEY, "foo");
        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo";
        System.setProperty(IstioConstant.SERVICE_NAME_KEY, "httpbin");

        System.setProperty("NAMESPACE", "foo");
        System.setProperty("SERVICE_NAME", "httpbin");
        System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        System.setProperty("SA_CA_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        System.setProperty(
                "SA_TOKEN_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_foo");

        ConfigurableApplicationContext context = SpringApplication.run(FooApplication.class, args);
        FooApplication application = context.getBean(FooApplication.class);
        application.sayHello();
        new CountDownLatch(1).await();
    }

    public void sayHello() throws InterruptedException {
        new Scanner(System.in).nextLine();
        while (true) {
            Thread.sleep(2000);
            System.out.println(demoService.sayHello("hello from foo"));
        }
    }
}
