/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufInvoker.java
 * 创建人:xichen
 * 创建日期:2015年12月20日-下午10:11:59
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;
import com.alibaba.dubbo.rpc.support.RpcUtils;

/**
 * 类名称:ProtobufInvoker 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月20日-下午10:11:59 修改备注:
 */
public class ProtobufInvoker<T> extends AbstractInvoker<T> {

  private final ExchangeClient[] clients;

  private final AtomicPositiveInteger index = new AtomicPositiveInteger();

  private final String version;

  private final ReentrantLock destroyLock = new ReentrantLock();

  private final Set<Invoker<?>> invokers;

  public ProtobufInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients) {
    this(serviceType, url, clients, null);
  }

  public ProtobufInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients, Set<Invoker<?>> invokers) {
    super(serviceType, url, new String[] {Constants.INTERFACE_KEY, Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
    this.clients = clients;
    // get version.
    this.version = url.getParameter(Constants.VERSION_KEY, "0.0.0");
    this.invokers = invokers;
  }

  @Override
  protected Result doInvoke(Invocation invocation) throws Throwable {
    RpcInvocation inv = (RpcInvocation) invocation;
    final String methodName = RpcUtils.getMethodName(invocation);
    inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
    inv.setAttachment(Constants.INTERFACE_KEY, getInterface().getName());
    inv.setAttachment(Constants.VERSION_KEY, version);

    ExchangeClient currentClient;
    if (clients.length == 1) {
      currentClient = clients[0];
    } else {
      currentClient = clients[index.getAndIncrement() % clients.length];
    }
    try {
      boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
      boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
      int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
      if (isOneway) {
        boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);
        currentClient.send(inv, isSent);
        RpcContext.getContext().setFuture(null);
        return new RpcResult();
      } else if (isAsync) {
        ResponseFuture future = currentClient.request(inv, timeout);
        RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));
        return new RpcResult();
      } else {
        RpcContext.getContext().setFuture(null);
        return (Result) currentClient.request(inv, timeout).get();
      }
    } catch (TimeoutException e) {
      throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: "
          + e.getMessage(), e);
    } catch (RemotingException e) {
      throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: "
          + e.getMessage(), e);
    }
  }

  @Override
  public boolean isAvailable() {
    if (!super.isAvailable())
      return false;
    for (ExchangeClient client : clients) {
      if (client.isConnected() && !client.hasAttribute(Constants.CHANNEL_ATTRIBUTE_READONLY_KEY)) {
        // cannot write == not Available ?
        return true;
      }
    }
    return false;
  }

  public void destroy() {
    // 防止client被关闭多次.在connect per jvm的情况下，client.close方法会调用计数器-1，当计数器小于等于0的情况下，才真正关闭
    if (super.isDestroyed()) {
      return;
    } else {
      // dubbo check ,避免多次关闭
      destroyLock.lock();
      try {
        if (super.isDestroyed()) {
          return;
        }
        super.destroy();
        if (invokers != null) {
          invokers.remove(this);
        }
        for (ExchangeClient client : clients) {
          try {
            client.close();
          } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
          }
        }

      } finally {
        destroyLock.unlock();
      }
    }
  }
}
