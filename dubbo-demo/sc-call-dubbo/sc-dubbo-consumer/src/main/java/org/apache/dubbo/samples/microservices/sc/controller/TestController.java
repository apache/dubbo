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
package org.apache.dubbo.samples.microservices.sc.controller;

import org.apache.dubbo.samples.microservices.sc.feign.TestFeign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/dubbo")
public class TestController {

    @Value("${spring.application.name}")
    private String appName;

    private final RestTemplate restTemplate;
    private final TestFeign testFeign;

    public TestController(RestTemplate restTemplate,
                          TestFeign testFeign) {
        this.restTemplate = restTemplate;
        this.testFeign = testFeign;
    }

    @RequestMapping("/rest/user")
    public String doRestAliveUsingEurekaAndRibbon() {
        String url = "http://dubbo-provider-for-sc/users/1";
        System.out.println("url: " + url);
        return restTemplate.getForObject(url, String.class);
    }

    @RequestMapping("/rest/user/feign")
    public String doRestAliveUsingFeign() {
        return testFeign.doAlive();
    }
}