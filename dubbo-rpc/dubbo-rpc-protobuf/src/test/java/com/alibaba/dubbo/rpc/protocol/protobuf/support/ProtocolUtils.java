/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf.support
 * 文件名:ProtocolUtils.java
 * 创建人:xichen
 * 创建日期:2015年12月25日-下午3:24:32
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf.support;

import java.util.Collection;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol;

/**
 * 类名称:ProtocolUtils 类描述:协议帮助类,简化代码 创建人:xichen 修改人:xichen 修改时间:2015年12月25日-下午3:24:32 修改备注:
 */
public class ProtocolUtils {
  private static Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
  public static ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

  public static <T> T refer(Class<T> type, String url) {
    return refer(type, URL.valueOf(url));
  }

  public static <T> T refer(Class<T> type, URL url) {
    return proxy.getProxy(protocol.refer(type, url));
  }

  public static Invoker<?> referInvoker(Class<?> type, URL url) {
    return (Invoker<?>) protocol.refer(type, url);
  }

  public static <T> Exporter<T> export(T instance, Class<T> type, String url) {
    return export(instance, type, URL.valueOf(url));
  }

  public static <T> Exporter<T> export(T instance, Class<T> type, URL url) {
    return protocol.export(proxy.getInvoker(instance, type, url));
  }

  public static void closeAll() {
    DubboProtocol.getDubboProtocol().destroy();
    Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
    for (ExchangeServer server : servers) {
      server.close();
    }
  }
}
