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
