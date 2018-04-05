package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.HttpDemoService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

@Configuration
public class Main {

//    http://www.baeldung.com/spring-remoting-http-invoker

    @Bean
    public HttpInvokerProxyFactoryBean invoker() {
        HttpInvokerProxyFactoryBean invoker = new HttpInvokerProxyFactoryBean();
        invoker.setServiceUrl("http://192.168.3.17:8080/com.alibaba.dubbo.demo.HttpDemoService");
        invoker.setServiceInterface(HttpDemoService.class);
        return invoker;
    }

    public static void main(String[] args) {
        HttpDemoService service = SpringApplication
                .run(Main.class, args)
                .getBean(HttpDemoService.class);
        System.out.println("666:" + service.hello("123"));
    }

}
