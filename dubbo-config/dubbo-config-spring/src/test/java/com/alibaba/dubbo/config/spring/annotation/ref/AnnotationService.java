package com.alibaba.dubbo.config.spring.annotation.ref;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.api.DemoService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

/**
 * @author cvictory ON 2018/10/16
 */
@Component
public class AnnotationService {

    @Reference(version = "1.2" , parameters = {"k1", "v1"})
    private DemoService demoService;

    public String doSayName(String name) {
        System.out.println("input:" + name);
        return demoService.sayName(name);
    }
}
