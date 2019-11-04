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