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

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.rpc.service.GenericService;

public class GenericConsumer {
	
	public static void main(String[] args) throws Throwable {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:generic_consumer.xml");      
        context.start();
        GenericService service = (GenericService)context.getBean("demoService");
        Map<Object, Object> address = new HashMap<Object, Object>();
        address.put("class", "com.alibaba.dubbo.demo.api.Address");
        address.put("city", "hangzhou");
        address.put("street", "wangshang");
        address.put("code", "310052");
        Map<Object, Object> user = new HashMap<Object, Object>();
		user.put("class", "com.alibaba.dubbo.demo.api.User");
		user.put("name", "liangfei");
		user.put("age", 25);
		user.put("role", "MEMBER");
		user.put("address", address);
		user.put("phones", new String[]{"13012345678", "1308654321"});
		for (int i = 0; i < Integer.MAX_VALUE; i ++) {
            Thread.sleep(2000);
            try {
				Object result = service.$invoke("sayHello", new String[]{"com.alibaba.dubbo.demo.api.User"}, new Object[]{user});
				System.out.println("-------- (" + i + ") " + result);
            } catch (Exception e) {
            	e.printStackTrace();
            }
        }
	}

}