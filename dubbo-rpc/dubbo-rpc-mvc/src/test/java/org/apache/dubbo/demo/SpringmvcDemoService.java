package org.apache.dubbo.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/demoService")
public interface SpringmvcDemoService {


    @GetMapping("/hello")
    Integer hello(@RequestParam("a") Integer a, @RequestParam("b") Integer b);


    @GetMapping("/error")
    String error();


    @PostMapping(value = "/say", consumes = "text/plain")
    String sayHello(String name);


    @GetMapping("/getRemoteApplicationName")
    String getRemoteApplicationName();

}
