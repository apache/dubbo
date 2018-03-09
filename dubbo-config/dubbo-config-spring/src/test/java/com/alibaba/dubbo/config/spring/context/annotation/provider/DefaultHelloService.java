package com.alibaba.dubbo.config.spring.context.annotation.provider;

import com.alibaba.dubbo.config.spring.api.HelloService;
import org.springframework.stereotype.Service;

/**
 * Default {@link HelloService} annotation with Spring's {@link Service}
 * and Dubbo's {@link com.alibaba.dubbo.config.annotation.Service}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since TODO
 */
@Service
@com.alibaba.dubbo.config.annotation.Service
public class DefaultHelloService implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Greeting, " + name;
    }

}
