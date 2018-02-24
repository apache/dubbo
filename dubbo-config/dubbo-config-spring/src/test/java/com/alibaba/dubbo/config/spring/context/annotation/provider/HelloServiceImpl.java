package com.alibaba.dubbo.config.spring.context.annotation.provider;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.api.HelloService;

/**
 * {@link HelloService} Implementation just annotating Dubbo's {@link Service}
 *
 * @since 2.5.9
 */
@Service
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
