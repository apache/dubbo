package com.alibaba.dubbo.config.spring.context.annotation.provider;

import com.alibaba.dubbo.config.spring.api.Box;
import com.alibaba.dubbo.config.spring.api.DemoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * {@link DemoService} Service implementation
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
@com.alibaba.dubbo.config.annotation.Service(
        version = "2.5.7",
        application = "${demo.service.application}",
        protocol = "${demo.service.protocol}",
        registry = "${demo.service.registry}"
)
@Service
@Transactional
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayName(String name) {
        return "Hello," + name;
    }

    @Override
    public Box getBox() {
        return new Box() {
            @Override
            public String getName() {
                return "MyBox";
            }
        };
    }
}
