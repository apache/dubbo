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

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.springboot.demo.DemoService;
import org.apache.dubbo.xds.istio.IstioConstant;

import java.util.Scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@SpringBootApplication
@Service
@EnableDubbo
public class BarApplication {

    //    @DubboReference(cluster = "xds", providedBy = "httpbin")
    @DubboReference(lazy = true,parameters = {"security","mTLS,sa_jwt"})
    private DemoService demoService;

    public static void main(String[] args) throws InterruptedException {
        System.setProperty(IstioConstant.WORKLOAD_NAMESPACE_KEY, "bar");
        System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        System.setProperty("SA_CA_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/ca.crt");
        System.setProperty(
                "SA_TOKEN_PATH", "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar");
        System.setProperty("NAMESPACE", "bar");
        IstioConstant.KUBERNETES_SA_PATH = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount/token_bar";
        System.setProperty(IstioConstant.SERVICE_NAME_KEY, "httpbin");

        ConfigurableApplicationContext context = SpringApplication.run(BarApplication.class, args);
        BarApplication application = context.getBean(BarApplication.class);
        String result = application.doSayHello();
        System.out.println("result: " + result);
    }

    public String doSayHello() throws InterruptedException {
        new Scanner(System.in).nextLine();
        while (true) {
            Thread.sleep(2000);
            System.out.println(demoService.sayHello("hello from bar"));
        }
    }
}
