package com.alibaba.dubbo.examples.redis;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

public class RedisConsumer {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String config = RedisConsumer.class.getPackage().getName().replace('.', '/') + "/redis-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        Map<String, Object> cache = (Map<String, Object>) context.getBean("cache");
        cache.remove("hello");
        Object value = cache.get("hello");
        System.out.println(value);
        if (value != null) {
            throw new IllegalStateException(value + " != null");
        }
        cache.put("hello", "world");
        value = cache.get("hello");
        System.out.println(value);
        if (!"world".equals(value)) {
            throw new IllegalStateException(value + " != world");
        }
    }

}
