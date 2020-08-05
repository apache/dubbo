package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.demo.GreetingService;

@DubboService
public class GreetingServiceImpl implements GreetingService {
    @Override
    public String hello() {
        return "收到附件是回到房间和接受对方健身房的就";
    }
}
