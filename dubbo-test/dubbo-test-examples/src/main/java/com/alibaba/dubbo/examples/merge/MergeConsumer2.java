package com.alibaba.dubbo.examples.merge;

import com.alibaba.dubbo.examples.merge.api.MergeService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;


public class MergeConsumer2 {

    public static void main(String[] args) throws Exception {
        String config = MergeConsumer2.class.getPackage().getName().replace('.', '/') + "/merge-consumer2.xml";
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
