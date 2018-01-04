/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.examples.loadbalance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.examples.generic.api.IUserService;
import com.alibaba.dubbo.rpc.cluster.support.FailoverCluster;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * LoadbalanceConsumer
 * 
 * @author yunfei.gyf
 */
public class LoadbalanceConsumer {

    public static void main(String[] args) throws Throwable {
        String config = LoadbalanceConsumer.class.getPackage().getName().replace('.', '/') + "/loadbalance-consumer.xml";
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(config);
        context.start();
        IUserService userservice = (IUserService) context.getBean("userservice");
        String r = userservice.getString("hello not generic");
        System.out.println(r);
        
        
        Map<String, String> methodconfig = new HashMap<String, String>();
        methodconfig.put("getString", "consistenthash");
        GenericService service = getService(methodconfig);
        Object result = service.$invoke("getString", new String[] {"java.lang.String"}, new Object[] {"hello generic"});
        System.out.println(result);
        
        result = service.$invoke("getString2", new String[] {"java.lang.String"}, new Object[] {"hello generic, 2"});
        System.out.println(result);
        
        context.close();
    }
    
    public static GenericService getService(Map<String, String> methodConfig) throws Throwable {
    		ApplicationConfig application = new ApplicationConfig();
    		application.setName("loadbalance-consumer");

    		// 连接注册中心配置
    		RegistryConfig registry = new RegistryConfig();
    		registry.setAddress("zookeeper://172.16.200.1:2181");

    		// 服务消费者缺省值配置
    		ConsumerConfig consumer = new ConsumerConfig();

    		ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();

    		reference.setApplication(application);
    		reference.setRegistry(registry);
    		reference.setConsumer(consumer);
    		reference.setCluster(FailoverCluster.NAME);
    		
    		List<MethodConfig> list = new ArrayList<MethodConfig>();
    		for (Entry<String, String> entry : methodConfig.entrySet()) {
    			MethodConfig mc = new MethodConfig();
    			mc.setName(entry.getKey());
    			mc.setLoadbalance(entry.getValue());
    			list.add(mc);
    		}
    		reference.setMethods(list);

    		reference.setInterface(IUserService.class); // 弱类型接口名
    		reference.setTimeout(3000);
    		reference.setRetries(0);
    		reference.setGeneric(true); // 声明为泛化接口

    		return reference.get();
    }
}
