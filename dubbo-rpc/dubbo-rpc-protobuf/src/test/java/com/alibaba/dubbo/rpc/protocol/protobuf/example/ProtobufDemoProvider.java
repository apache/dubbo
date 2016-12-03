/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf.example
 * 文件名:ProtobufDemoProvider.java
 * 创建人:xichen
 * 创建日期:2015年12月23日-下午5:16:37
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf.example;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 类名称:ProtobufDemoProvider 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月23日-下午5:16:37 修改备注:
 */
public class ProtobufDemoProvider {

  public static void main(String[] args) throws Exception {
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("protobuf-demo-provider.xml");
    context.start();
    System.out.println("context started");
    System.in.read();
  }

}
