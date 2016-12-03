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
