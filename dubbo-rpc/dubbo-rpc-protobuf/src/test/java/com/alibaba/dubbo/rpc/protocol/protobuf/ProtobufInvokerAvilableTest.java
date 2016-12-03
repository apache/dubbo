/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufInvokerAvilableTest.java
 * 创建人:xichen
 * 创建日期:2015年12月25日-下午3:05:49
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import java.lang.reflect.Field;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoImpl;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto;
import com.alibaba.dubbo.rpc.protocol.protobuf.support.ProtocolUtils;

/**
 * 类名称:ProtobufInvokerAvilableTest 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月25日-下午3:05:49 修改备注:
 */
public class ProtobufInvokerAvilableTest {

  private Protocol protocol = ProtobufProtocol.getProtobufProtocol();
  private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

  private URL url = URL.valueOf("protobuf://127.0.0.1:9021/" + EchoProto.EchoService.BlockingInterface.class.getName());

  @Test
  public void test_Normal_available() {

    ProtocolUtils.export(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url);

    ProtobufInvoker<?> invoker = (ProtobufInvoker<?>) protocol.refer(EchoProto.EchoService.BlockingInterface.class, url);

    Assert.assertEquals(true, invoker.isAvailable());
    invoker.destroy();
    Assert.assertEquals(false, invoker.isAvailable());
  }

  @Test
  public void test_Normal_ChannelReadOnly() throws Exception {

    ProtocolUtils.export(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url);

    ProtobufInvoker<?> invoker = (ProtobufInvoker<?>) protocol.refer(EchoProto.EchoService.BlockingInterface.class, url);
    Assert.assertEquals(true, invoker.isAvailable());

    getClients(invoker)[0].setAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY, Boolean.TRUE);

    Assert.assertEquals(false, invoker.isAvailable());

    // 恢复状态，invoker共享连接
    getClients(invoker)[0].removeAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY);
  }

  @Test
  public void test_NoInvokers() throws Exception {

    ProtocolUtils.export(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url);

    ProtobufInvoker<?> invoker = (ProtobufInvoker<?>) protocol.refer(EchoProto.EchoService.BlockingInterface.class, url);

    ExchangeClient[] clients = getClients(invoker);
    clients[0].close();
    Assert.assertEquals(false, invoker.isAvailable());

  }

  private ExchangeClient[] getClients(ProtobufInvoker<?> invoker) throws Exception {
    Field field = ProtobufInvoker.class.getDeclaredField("clients");
    field.setAccessible(true);
    ExchangeClient[] clients = (ExchangeClient[]) field.get(invoker);
    Assert.assertEquals(1, clients.length);
    return clients;
  }
}
