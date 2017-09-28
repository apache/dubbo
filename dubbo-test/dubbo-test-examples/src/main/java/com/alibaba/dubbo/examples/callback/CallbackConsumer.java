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
package com.alibaba.dubbo.examples.callback;

import com.alibaba.dubbo.examples.callback.api.CallbackListener;
import com.alibaba.dubbo.examples.callback.api.CallbackService;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * CallbackConsumer
 *
 * @author william.liangf
 */
public class CallbackConsumer {

    public static void main(String[] args) throws Exception {
        String config = CallbackConsumer.class.getPackage().getName().replace('.', '/') + "/callback-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        CallbackService callbackService = (CallbackService) context.getBean("callbackService");
        callbackService.addListener("foo.bar", new CallbackListener() {
            public void changed(String msg) {
                System.out.println("callback1:" + msg);
            }
        });
        System.in.read();
    }

}
