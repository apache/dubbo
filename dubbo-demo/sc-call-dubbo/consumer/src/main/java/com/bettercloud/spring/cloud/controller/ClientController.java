package com.bettercloud.spring.cloud.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
@RequestMapping("/client")
public class ClientController {

    private String serviceEndpoint;
    private RestTemplate restTemplate;

    @Autowired
    public ClientController(@Value("${service.me}") final String serviceEndpoint, final RestTemplate restTemplate) {
        this.serviceEndpoint = serviceEndpoint;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public String getService() {
        return restTemplate.getForEntity(serviceEndpoint, String.class).getBody();
    }

}
