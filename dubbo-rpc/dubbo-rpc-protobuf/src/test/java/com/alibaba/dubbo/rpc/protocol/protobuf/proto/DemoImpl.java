/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf.proto
 * 文件名:DemoImpl.java
 * 创建人:xichen
 * 创建日期:2015年12月28日-下午2:12:46
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf.proto;

import com.alibaba.dubbo.rpc.protocol.protobuf.proto.DemoProto.EchoRequest;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.DemoProto.EchoResponse;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * 类名称:DemoImpl 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月28日-下午2:12:46 修改备注:
 */
public class DemoImpl implements DemoProto.DemoService.BlockingInterface {

  /*
   * 测试回报接口
   */
  public EchoResponse echo(RpcController controller, EchoRequest request) throws ServiceException {

    // System.out.println("...EchoImpl.....echo data:" + request.getData());

    return EchoResponse.newBuilder().setResult(request.getData()).build();

  }


}
