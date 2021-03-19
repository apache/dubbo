package org.apache.dubbo.config.spring.issues.issue6000.dubbo;

import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.api.HelloService;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration class is considered to be initialized after HelloDubbo,
 * but the reference bean defined in it can be injected into HelloDubbo
 */
@Configuration
public class MyReferenceConfig {
    @Reference(version = "1.0.0", check = false)
    HelloService helloService;
}