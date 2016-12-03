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
package com.alibaba.dubbo.rpc.protocol.protobuf.proto;

import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto.EchoRequest;
import com.alibaba.dubbo.rpc.protocol.protobuf.proto.EchoProto.EchoResponse;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * 类名称:EchoImpl 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月22日-下午10:31:40 修改备注:
 */
public class EchoImpl implements EchoProto.EchoService.BlockingInterface {

  /*
   * 测试回报接口
   */
  public EchoResponse echo(RpcController controller, EchoRequest request) throws ServiceException {

    // System.out.println("...EchoImpl.....echo data:" + request.getData());

    return EchoResponse.newBuilder().setResult(request.getData()).build();

  }

}
