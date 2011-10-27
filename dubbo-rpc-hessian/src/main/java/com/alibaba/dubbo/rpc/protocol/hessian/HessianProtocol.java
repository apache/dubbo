package com.alibaba.dubbo.rpc.protocol.hessian;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.JettyHttpServer;
import com.alibaba.dubbo.remoting.http.ServiceDispatcherServlet;
import com.alibaba.dubbo.remoting.http.ServletHttpServer;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProtocol;

/**
 * http rpc support.
 * 
 * @author qianlei
 */
@Extension("hessian")
public class HessianProtocol extends AbstractProtocol {

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private ProxyFactory                  proxyFactory;

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public int getDefaultPort() {
        return 80;
    }

    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();

        final String uri = url.getPath(); // service uri also exporter cache key.

        int threads = url.getIntParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);

        String addr = url.getHost() + ":" + url.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            String type = url.getParameter(Constants.SERVER_KEY, "jetty");
            if ("servlet".equals(type)) {
                server = new ServletHttpServer(url.getPort());
            } else if ("jetty".equals(type)) {
                // 和Dubbo协议一样，总是绑定到0.0.0.0上
                server = new JettyHttpServer(url.getPort(), threads);
            } else {
                throw new IllegalArgumentException("Unsupported http server " + type
                        + ", only support servlet, jetty!");
            }
            server.start();
            serverMap.put(addr, server);
        }

        HessianRpcExporter<T> exporter = new HessianRpcExporter<T>(invoker) {
            public void unexport() {
                super.unexport();
                exporterMap.remove(uri);
            }
        };
        exporterMap.put(uri, exporter);
        ServiceDispatcherServlet.addProcessor(url.getPort(), uri, exporter);
        return exporter;
    }

    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        Invoker<T> invoker = new HessianRpcInvoker<T>(serviceType, url, proxyFactory);
        invokers.add(invoker);
        return invoker;
    }

    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<String>(serverMap.keySet())) {
            HttpServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close hessian server 0.0.0.0:" + server.getPort());
                    }
                    server.stop();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

}
