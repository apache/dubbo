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
package com.alibaba.dubbo.demo.consumer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.rpc.RpcContext;

public class DemoAction {
    
    private DemoService demoService;

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }

	public void start() throws InterruptedException, ExecutionException {
		Future<String> f = RpcContext.getContext().asyncCall(new Callable<String>() {
			public String call() throws Exception {
				return demoService.sayHello("async call request");
			}
			
		});
		
		System.out.println("async call ret :" + f.get());
		
		RpcContext.getContext().asyncCall(new Runnable() {
			public void run() {
				demoService.sayHello("oneway call request");
				demoService.sayHello("oneway call request");
			}
		});
		
        for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            try {
            	String hello = demoService.sayHello("world" + i);
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + hello);
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
	}

}