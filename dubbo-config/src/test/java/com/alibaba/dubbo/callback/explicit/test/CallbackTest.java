/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.callback.explicit.test;


import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.registry.support.SimpleRegistryExporter;

/**
 * @author chao.liuc
 *
 */
public class CallbackTest {
    //test 开关
    private boolean test = true ;
    
    @Before
    public void setUp() throws Exception {
        test = false;
        SimpleRegistryExporter.exportIfAbsent(9234);
    }
    
    @Test
    public void startProvider() throws InterruptedException{
        if (!test) return;
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:com/alibaba/dubbo/callback/explicit/provider.xml");
        context.start();
        synchronized (CallbackTest.class) {
            CallbackTest.class.wait();
        }
    }
    
    @Test
    public void startConsumer() throws InterruptedException{
        if (!test) return;
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:com/alibaba/dubbo/callback/explicit/consumer.xml");
        context.start();
        
        IDemoService demoService = (IDemoService)context.getBean("demoService");
        
        IDemoCallback callback = new IDemoCallback(){
            public String yyy(String msg) {
                System.out.println("callback1:"+msg);
                return "callback1 onChanged ,"+msg;
            }
        };
        
        IDemoCallback callback2 = new IDemoCallback(){
            public String yyy(String msg) {
                System.out.println("callback2:"+msg);
                return "callback2 onChanged ,"+msg;
            }
        };
        
        int i = 1;
        long time = System.currentTimeMillis();
        System.out.println("cient request id:"+time);
        demoService.xxx(callback,"time:"+(i++) +",client request id:"+time);
        Thread.sleep(2500);
//        demoService.unxxx(callback,"unxxx");
        System.out.println("");
        
        time = System.currentTimeMillis();
        System.out.println("cient request id:"+time);
        demoService.xxx(callback2,"time:"+(i++) +",client request id:"+time);
        Thread.sleep(2500);
//        demoService.unxxx(callback2,"unxxx2");
        System.out.println("");
        
        time = System.currentTimeMillis();
        System.out.println("cient request id:"+time);
        demoService.xxx(callback,"time:"+(i++) +",client request id:"+time);
        Thread.sleep(2500);
//        demoService.unxxx(callback,"unxxx");
        
        synchronized (CallbackTest.class) {
            CallbackTest.class.wait();
        }
    }
}