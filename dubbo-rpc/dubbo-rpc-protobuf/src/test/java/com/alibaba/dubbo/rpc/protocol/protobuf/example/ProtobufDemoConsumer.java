/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf.example
 * 文件名:ProtobufDemoConsumer.java
 * 创建人:xichen
 * 创建日期:2015年12月23日-下午5:16:54
 * Copyright (c) 2015 Illuminate 公司-版权所有
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
