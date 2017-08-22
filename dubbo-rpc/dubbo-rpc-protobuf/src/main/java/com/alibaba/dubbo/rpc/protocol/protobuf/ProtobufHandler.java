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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;

/**
 * 类名称:ProtobufHandler 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月21日-下午9:45:04 修改备注:
 */
public class ProtobufHandler extends ExchangeHandlerAdapter {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  // 处理接受消息
  public Object reply(ExchangeChannel channel, Object message) throws RemotingException {
    if (message instanceof Invocation) {
      Invocation inv = (Invocation) message;
      int port = channel.getLocalAddress().getPort();
      // String path = inv.getAttachments().get(Constants.PATH_KEY);
      String serviceName = inv.getAttachments().get(Constants.INTERFACE_KEY);
      String serviceKey = ProtocolUtils.serviceKey(port, serviceName, inv.getAttachments().get(Constants.VERSION_KEY), inv.getAttachments().get(Constants.GROUP_KEY));

      ProtobufExporter<?> exporter = (ProtobufExporter<?>) ProtobufProtocol.getProtobufProtocol().getExporterMap().get(serviceKey);
      if (exporter == null) {
        throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + ProtobufProtocol.getProtobufProtocol().getExporterMap().keySet()
            + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + message);
      }
      RpcContext.getContext().setRemoteAddress(channel.getRemoteAddress());
      return exporter.getInvoker().invoke(inv);
    }
    throw new RemotingException(channel, "Unsupported request: " + message == null ? null : (message.getClass().getName() + ": " + message) + ", channel: consumer: "
        + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
  }

  @Override
  public void received(Channel channel, Object message) throws RemotingException {
    if (message instanceof Invocation) {
      reply((ExchangeChannel) channel, message);
    } else {
      super.received(channel, message);
    }
  }

  @Override
  public void connected(Channel channel) throws RemotingException {
    invoke(channel, Constants.ON_CONNECT_KEY);
  }

  @Override
  public void disconnected(Channel channel) throws RemotingException {
    if (logger.isInfoEnabled()) {
      logger.info("disconected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
    }
    invoke(channel, Constants.ON_DISCONNECT_KEY);
  }

  private void invoke(Channel channel, String methodKey) {
    Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
    if (invocation != null) {
      try {
        received(channel, invocation);
      } catch (Throwable t) {
        logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
      }
    }
  }

  private Invocation createInvocation(Channel channel, URL url, String methodKey) {
    String method = url.getParameter(methodKey);
    if (method == null || method.length() == 0) {
      return null;
    }
    RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
    invocation.setAttachment(Constants.PATH_KEY, url.getPath());
    invocation.setAttachment(Constants.GROUP_KEY, url.getParameter(Constants.GROUP_KEY));
    invocation.setAttachment(Constants.INTERFACE_KEY, url.getParameter(Constants.INTERFACE_KEY));
    invocation.setAttachment(Constants.VERSION_KEY, url.getParameter(Constants.VERSION_KEY));
    if (url.getParameter(Constants.STUB_EVENT_KEY, false)) {
      invocation.setAttachment(Constants.STUB_EVENT_KEY, Boolean.TRUE.toString());
    }
    return invocation;
  }
}
