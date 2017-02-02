package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.rpc.protocol.proxy.GenericServiceConfig;
import com.alibaba.dubbo.rpc.protocol.proxy.ProxyService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;

/**
 * Created by wuyu on 2017/2/2.
 */
public class RestProxyConsumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:META-INF/spring/dubbo-demo-consumer.xml");
        ProxyService proxyService = ctx.getBean(ProxyService.class);
        GenericServiceConfig config = new GenericServiceConfig("com.alibaba.dubbo.demo.DemoService", "sayHello", new String[]{"wuyu"}, new String[]{"java.lang.String"});
        String proxy = (String) proxyService.invoke(config, String.class);
        System.err.println(proxy);

        config = new GenericServiceConfig("com.alibaba.dubbo.demo.DemoService", "insert", new String[]{JSON.toJSONString(new User("1", "wuyu"))}, new String[]{"com.alibaba.dubbo.demo.User"});

        User user = (User) proxyService.invoke(config, User.class);
        System.err.println(user);

        config = new GenericServiceConfig("com.alibaba.dubbo.demo.DemoService", "batchInsert", new String[]{JSON.toJSONString(Arrays.asList(new User("1", "wuyu"), new User("2", "wuyu")))}, null);
        List<User> users = (List<User>) proxyService.invoke(config, new TypeReference<List<User>>(){}.getType());
        System.err.println(users);

        DemoService demoservice = proxyService.target(DemoService.class);
        String wuyu = demoservice.sayHello("wuyu");
        System.err.println(wuyu);

        User wuyu1 = demoservice.insert(new User("1", "wuyu"));
        System.err.println(wuyu1);

        List<User> users1 = demoservice.batchInsert(Arrays.asList(new User("1", "wuyu"), new User("2", "zhangsan")));
        System.err.println(users1);
    }
}
