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
package org.apache.dubbo.demo.protobuf.consumer;


import org.apache.dubbo.demo.protobuf.api.GooglePb.GooglePBRequestType;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Application {
  /**
   * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
   * launch the application
   */
  public static void main(String[] args) {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("spring/dubbo-consumer.xml");
    context.start();
    GenericService genericService = context.getBean("demoService", GenericService.class);
    String methodName = "sayHello";
    String[] requestType = new String[]{GooglePBRequestType.class.getName()};

    // TODO this requestStr could generate from serviceMetaData. a new TypeDefinition is create will push next
    String requestStr = "{ \"req\": \"some Message\" }";
    Object[] request = new Object[]{requestStr};
    String hello = (String) genericService.$invoke(methodName,requestType,request);
    System.out.println("result: " + hello);
  }
}
