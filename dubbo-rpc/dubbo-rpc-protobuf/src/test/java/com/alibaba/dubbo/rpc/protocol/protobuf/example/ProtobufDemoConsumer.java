/**
 * Copyright (c) 2015 Illuminate inc.
 *
 * * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.alibaba.dubbo.rpc.protocol.protobuf.example;

import java.util.Random;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.alibaba.dubbo.rpc.protocol.protobuf.ProtobufController;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto;

/**
 * 类名称:ProtobufDemoConsumer 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月23日-下午5:16:54 修改备注:
 */
public class ProtobufDemoConsumer {

  private static ProtobufController controller = new ProtobufController();
  private static Random random = new Random();

  public static void main(String[] args) throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("protobuf-demo-consumer.xml");
    context.start();

    EchoProto.EchoService.BlockingInterface demo = (EchoProto.EchoService.BlockingInterface) context.getBean("demoService");

    // Create the request
    EchoProto.EchoRequest request = EchoProto.EchoRequest.newBuilder().setData(random.nextInt()).build();

    // display the echo response
    for (int i = 0; i < 10; i++) {
      System.out.println(demo.echo(controller, request).getResult());
    }

    context.close();
  }
}
