package com.alibaba.dubbo.examples.merge;

import com.alibaba.dubbo.examples.merge.api.MergeService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;


public class MergeConsumer {

    public static void main(String[] args) throws Exception {
        String config = MergeConsumer.class.getPackage().getName().replace('.', '/') + "/merge-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        MergeService mergeService = (MergeService) context.getBean("mergeService");
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            try {
                List<String> result = mergeService.mergeResult();
                System.out.println("(" + i + ") " + result);
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
