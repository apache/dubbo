/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.examples.merge;

import com.alibaba.dubbo.examples.merge.api.MergeService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

/**
 * MergeConsumer
 *
 * @author william.liangf
 */
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
