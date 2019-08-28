package org.apache.dubbo.samples.microservices.sc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private String instanceId;

    @Autowired
    public MeController(@Value("${spring.cloud.consul.discovery.instance-id:${random.value}}") String instanceId) {
        this.instanceId = instanceId;
    }

    @GetMapping("/me")
    public String me() {
        return instanceId;
    }

}
