/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufServicesTest.java
 * 创建人:xichen
 * 创建日期:2015年12月22日-下午10:27:12
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import junit.framework.Assert;

import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoImpl;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto;

/**
 * 类名称:ProtobufServicesTest 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月22日-下午10:27:12 修改备注:
 */
public class ProtobufServicesTest {

  // @Test
  public void testAddService() throws Exception {

    EchoProto.EchoService.BlockingInterface testInterface = new EchoImpl();

    Class[] testIns = testInterface.getClass().getInterfaces();

    ProtobufServices.getInstance().addService(testIns[0].getName());

    Assert.assertEquals(1, ProtobufServices.getInstance().getRequestCount());

    Assert.assertEquals(1, ProtobufServices.getInstance().getResponseCount());
  }
}
