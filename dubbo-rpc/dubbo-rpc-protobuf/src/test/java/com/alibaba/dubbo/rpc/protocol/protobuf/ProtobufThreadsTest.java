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

import static junit.framework.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoImpl;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto;
import com.alibaba.dubbo.rpc.proxy.jdk.JdkProxyFactory;
import com.google.protobuf.ServiceException;

/**
 * 类名称:ProtobufThreadsTest 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月28日-下午2:56:01 修改备注:
 */
public class ProtobufThreadsTest {

  private Protocol protocol = ProtobufProtocol.getProtobufProtocol();
  private ProxyFactory proxy = new JdkProxyFactory();

  private ProtobufController controller = new ProtobufController();
  private Random random = new Random();

  @Test
  public void testMultiThreadInvoke() throws ServiceException, InterruptedException {
    URL url = URL.valueOf("protobuf://127.0.0.1:9021/" + EchoProto.EchoService.BlockingInterface.class.getName());

    Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(new EchoImpl(), EchoProto.EchoService.BlockingInterface.class, url));

    final AtomicInteger counter = new AtomicInteger();
    final EchoProto.EchoService.BlockingInterface service = proxy.getProxy(protocol.refer(EchoProto.EchoService.BlockingInterface.class, url));

    // Create the request
    final EchoProto.EchoRequest request = EchoProto.EchoRequest.newBuilder().setData(random.nextInt(100)).build();

    // check response data
    assertEquals(service.echo(controller, request).getResult(), request.getData());

    // start 10 threads
    ExecutorService exec = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 10; i++) {
      final int fi = i;
      exec.execute(new Runnable() {
        public void run() {
          for (int i = 0; i < 30; i++) {
            // System.out.println(fi + ":" + counter.getAndIncrement());
            try {
              assertEquals(service.echo(controller, request).getResult(), request.getData());
            } catch (ServiceException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }
      });
    }
    exec.shutdown();
    exec.awaitTermination(10, TimeUnit.SECONDS);
    rpcExporter.unexport();
  }
}
