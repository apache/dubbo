package org.apache.dubbo.config.spring.boot.conditional7.consumer;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.HelloService;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * verify @Component with @Bean method
 */
@Order(Integer.MAX_VALUE-2)
@Component
public class AnnotationBeanConfiguration {
    //TEST Conditional, this bean should be ignored
    @Bean
    @DubboReference(group = "${myapp.group}", init = false)
    public  ReferenceBean<HelloService> helloService() {
        return new ReferenceBean();
    }
}
