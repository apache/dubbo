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
package com.alibaba.dubbo.examples.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.examples.async.api.AsyncService;
import com.alibaba.dubbo.rpc.RpcContext;

/**
 * CallbackConsumer
 * 
 * @author william.liangf
 */
public class AsyncConsumer {

    public static void main(String[] args) throws Exception {
        String config = AsyncConsumer.class.getPackage().getName().replace('.', '/') + "/async-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        
        final AsyncService asyncService = (AsyncService)context.getBean("asyncService");
        
        Future<String> f = RpcContext.getContext().asyncCall(new Callable<String>() {
            public String call() throws Exception {
                return asyncService.sayHello("async call request");
            }
        });
        
        System.out.println("async call ret :" + f.get());
        
        RpcContext.getContext().asyncCall(new Runnable() {
            public void run() {
                asyncService.sayHello("oneway call request1");
                asyncService.sayHello("oneway call request2");
            }
        });
        
        System.in.read();
    }

}
