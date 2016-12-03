/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufProtocol.java
 * 创建人:xichen
 * 创建日期:2015年12月20日-下午9:47:51
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Transporter;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * 类名称:ProtobufProtocol 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月20日-下午9:47:51 修改备注:
 */
public class ProtobufProtocol extends AbstractProtocol {

  public static final int DEFAULT_PORT = 40888;

  public static final String NAME = "protobuf";

  // key为ip:port，value映射监听服务
  private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<String, ExchangeServer>(); // <host:port,Exchanger>

  // 请求回报处理逻辑
  private ExchangeHandler requestHandler = new ProtobufHandler();

  // 单例模式
  private static ProtobufProtocol INSTANCE;

  public ProtobufProtocol() {
    INSTANCE = this;
  }

  public static ProtobufProtocol getProtobufProtocol() {
    if (INSTANCE == null) {
      ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(ProtobufProtocol.NAME); // load
    }
    return INSTANCE;
  }

  public Collection<ExchangeServer> getServers() {
    return Collections.unmodifiableCollection(serverMap.values());
  }

  public Collection<Exporter<?>> getExporters() {
    return Collections.unmodifiableCollection(exporterMap.values());
  }

  public Collection<Invoker<?>> getInvokers() {
    return Collections.unmodifiableCollection(invokers);
  }

  Map<String, Exporter<?>> getExporterMap() {
    return exporterMap;
  }

  /*
   * 获取默认端口号
   */
  public int getDefaultPort() {
    return DEFAULT_PORT;
  }

  /*
   * 输出service
   */
  public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
    URL url = invoker.getUrl();

    // register service
    ProtobufServices.getInstance().addService(invoker);

    // export service.
    String key = serviceKey(url);
    ProtobufExporter<T> exporter = new ProtobufExporter<T>(invoker, key, exporterMap);
    exporterMap.put(key, exporter);

    // start service
    openServer(url);

    return exporter;
  }

  /*
   * 输出调用者
   */
  public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {

    // register service
    ProtobufServices.getInstance().addService(serviceType);

    // create rpc invoker.
    ProtobufInvoker<T> invoker = new ProtobufInvoker<T>(serviceType, url, getClients(url), invokers);
    invokers.add(invoker);
    return invoker;
  }

  public void destroy() {

    super.destroy();

    for (String key : new ArrayList<String>(serverMap.keySet())) {

      ExchangeServer server = serverMap.remove(key);

      if (server != null) {
        try {
          if (logger.isInfoEnabled()) {
            logger.info("Close protobuf server: " + server.getLocalAddress());
          }
          server.close(getServerShutdownTimeout());
        } catch (Throwable t) {
          logger.warn(t.getMessage(), t);
        }
      } // ~ end of if ( server != null )

    } // ~ end of loop serverMap

  } // ~ end of method destroy

  private void openServer(URL url) {
    // find server.
    String key = url.getAddress();
    // client 也可以暴露一个只有server可以调用的服务。
    boolean isServer = url.getParameter(Constants.IS_SERVER_KEY, true);
    if (isServer) {
      ExchangeServer server = serverMap.get(key);
      if (server == null) {
        serverMap.put(key, createServer(url));
      } else {
        // server支持reset,配合override功能使用
        server.reset(url);
      }
    }
  }

  private ExchangeServer createServer(URL url) {
    // 默认开启server关闭时发送readonly事件
    url = url.addParameterIfAbsent(Constants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString());

    // BIO存在严重性能问题，暂时不允许使用
    String str = url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_SERVER);
    if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str))
      throw new RpcException("Unsupported server type: " + str + ", url: " + url);

    // 默认开启heartbeat
    url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));
    url = url.addParameter(Constants.CODEC_KEY, ProtobufCodec.NAME);

    ExchangeServer server;
    try {
      server = Exchangers.bind(url, requestHandler);
    } catch (RemotingException e) {
      throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
    }

    str = url.getParameter(Constants.CLIENT_KEY);
    if (str != null && str.length() > 0) {
      Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
      if (!supportedTypes.contains(str)) {
        throw new RpcException("Unsupported client type: " + str);
      }
    }
    return server;
  }


  private ExchangeClient[] getClients(URL url) {

    int connections = url.getParameter(Constants.CONNECTIONS_KEY, 1);

    ExchangeClient[] clients = new ExchangeClient[connections];

    for (int i = 0; i < clients.length; i++) {
      clients[i] = initClient(url);
      logger.info("succeed to create remoteing client for service("+url + ")");;
    }
    return clients;
  }

  private ExchangeClient initClient(URL url) {

    // client type setting.
    String str = url.getParameter(Constants.CLIENT_KEY, url.getParameter(Constants.SERVER_KEY, Constants.DEFAULT_REMOTING_CLIENT));
    // BIO存在严重性能问题，暂时不允许使用
    if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
      throw new RpcException("Unsupported client type: " + str + "," + " supported client type is "
          + StringUtils.join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
    }

    // 默认开启heartbeat
    url = url.addParameterIfAbsent(Constants.HEARTBEAT_KEY, String.valueOf(Constants.DEFAULT_HEARTBEAT));
    url = url.addParameter(Constants.CODEC_KEY, ProtobufCodec.NAME);

    ExchangeClient client;
    try {
      client = Exchangers.connect(url);
    } catch (RemotingException e) {
      throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
    }
    return client;
  }

}
