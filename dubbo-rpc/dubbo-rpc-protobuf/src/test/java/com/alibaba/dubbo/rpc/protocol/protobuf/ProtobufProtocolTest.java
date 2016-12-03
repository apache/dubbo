/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufProtocolTest.java
 * 创建人:xichen
 * 创建日期:2015年12月25日-下午2:40:43
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.DemoImpl;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.DemoProto;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoImpl;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto;
import com.alibaba.dubbo.rpc.proxy.jdk.JdkProxyFactory;
import com.google.protobuf.ServiceException;

/**
 * 类名称:ProtobufProtocolTest 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月25日-下午2:40:43 修改备注:
 */
public class ProtobufProtocolTest {

  private Protocol protocol = ProtobufProtocol.getProtobufProtocol();
  private ProxyFactory proxy = new JdkProxyFactory();

  private ProtobufController controller = new ProtobufController();
  private Random random = new Random();

  @Test
  public void testSyncProtocol() throws Exception {
    URL url = URL.valueOf("protobuf://127.0.0.1:9021/" + EchoProto.EchoService.BlockingInterface.class.getName());

    protocol.export(proxy.getInvoker(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url));

    EchoProto.EchoService.BlockingInterface service = proxy.getProxy(protocol.refer(EchoProto.EchoService.BlockingInterface.class, url));

    // Create the request
    EchoProto.EchoRequest request = EchoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();

    // check response data
    assertEquals(service.echo(controller, request).getResult(), request.getData());
  }

  @Test
  public void testAsyncProtocol() throws ServiceException, InterruptedException, ExecutionException {
    URL url = URL.valueOf("protobuf://127.0.0.1:9022/" + EchoProto.EchoService.BlockingInterface.class.getName() + "?async=true&timeout=1000");

    protocol.export(proxy.getInvoker(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url));

    EchoProto.EchoService.BlockingInterface service = proxy.getProxy(protocol.refer(EchoProto.EchoService.BlockingInterface.class, url));

    EchoProto.EchoRequest request = EchoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();

    service.echo(controller, request);

    Future<EchoProto.EchoResponse> echoFuture = RpcContext.getContext().getFuture();

    assertNotNull(echoFuture);

    Thread.sleep(500);

    assertEquals(echoFuture.get().getResult(), request.getData());

    // Thread.sleep(800);

    // assertNull(RpcContext.getContext().getFuture());
  }

  @Test
  public void testPushProtocol() throws ServiceException, InterruptedException, ExecutionException {
    URL url = URL.valueOf("protobuf://127.0.0.1:9023/" + EchoProto.EchoService.BlockingInterface.class.getName() + "?return=true&timeout=1000");

    protocol.export(proxy.getInvoker(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url));

    EchoProto.EchoService.BlockingInterface service = proxy.getProxy(protocol.refer(EchoProto.EchoService.BlockingInterface.class, url));

    EchoProto.EchoRequest request = EchoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();

    service.echo(controller, request);

    assertNull(RpcContext.getContext().getFuture());
  }

  @Test
  public void testProtocolMultiService() throws Exception {
    URL echoUrl = URL.valueOf("protobuf://127.0.0.1:9024/" + EchoProto.EchoService.BlockingInterface.class.getName());
    URL demoUrl = URL.valueOf("protobuf://127.0.0.1:9024/" + DemoProto.DemoService.BlockingInterface.class.getName());

    EchoProto.EchoService.BlockingInterface service = new EchoImpl();
    protocol.export(proxy.getInvoker(service, EchoProto.EchoService.BlockingInterface.class, echoUrl));
    service = proxy.getProxy(protocol.refer(EchoProto.EchoService.BlockingInterface.class, echoUrl));

    DemoProto.DemoService.BlockingInterface remote = new DemoImpl();
    protocol.export(proxy.getInvoker(remote, DemoProto.DemoService.BlockingInterface.class, demoUrl));
    remote = proxy.getProxy(protocol.refer(DemoProto.DemoService.BlockingInterface.class, demoUrl));

    // Create the request
    EchoProto.EchoRequest echoRequest = EchoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();
    DemoProto.EchoRequest demoRequest = DemoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();

    // test netty client
    assertEquals(service.echo(controller, echoRequest).getResult(), echoRequest.getData());
    assertEquals(remote.echo(controller, demoRequest).getResult(), demoRequest.getData());
  }

  @Test
  public void testPerm() throws Exception {
    URL url = URL.valueOf("protobuf://127.0.0.1:9025/" + EchoProto.EchoService.BlockingInterface.class.getName());

    EchoProto.EchoService.BlockingInterface service = new EchoImpl();
    protocol.export(proxy.getInvoker(service, EchoProto.EchoService.BlockingInterface.class, url));
    service = proxy.getProxy(protocol.refer(EchoProto.EchoService.BlockingInterface.class, url));

    // Create the request
    EchoProto.EchoRequest request = EchoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();

    final int testNums = 1000;

    long start = System.currentTimeMillis();
    for (int i = 0; i < testNums; i++)
      service.echo(controller, request);
    System.out.println(testNums + " echos take:" + (System.currentTimeMillis() - start));
  }
}
