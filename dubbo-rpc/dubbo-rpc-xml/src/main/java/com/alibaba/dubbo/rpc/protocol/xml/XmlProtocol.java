package com.alibaba.dubbo.rpc.protocol.xml;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/4.
 */
public class XmlProtocol extends AbstractProxyProtocol {

    private final Map<Integer, XmlRpcServletServer> serverMap = new ConcurrentHashMap<>();

    private HttpBinder httpBinder;

    @Override
    public int getDefaultPort() {
        return 8080;
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {
        final Integer port = url.getPort();
        XmlRpcHandlerMappingImpl phm = new XmlRpcHandlerMappingImpl();

        T bean = ServiceBean.getSpringContext().getBean(type);
        try {
            XmlRpcServletServer xmlRpcServletServer = serverMap.get(port);
            if (xmlRpcServletServer == null) {
                xmlRpcServletServer = new XmlRpcServletServer();
                XmlRpcServerConfigImpl xmlRpcServerConfig = new XmlRpcServerConfigImpl();

                xmlRpcServerConfig.setEnabledForExtensions(true);
                xmlRpcServerConfig.setKeepAliveEnabled(true);
                xmlRpcServerConfig.setEnabledForExceptions(true);
                xmlRpcServletServer.setConfig(xmlRpcServerConfig);
                httpBinder.bind(url, new XmlHttpHandler());
                serverMap.put(port, xmlRpcServletServer);
            }
            phm.addHandler(type.getName(), bean.getClass(), bean);
            xmlRpcServletServer.setHandlerMapping(phm);
        } catch (XmlRpcException e) {
            throw new RpcException(e);
        }

        return new Runnable() {
            @Override
            public void run() {
                XmlRpcServletServer xmlRpcServletServer = serverMap.get(port);
                if (xmlRpcServletServer != null) {
                    PropertyHandlerMapping propertyHandlerMapping = (PropertyHandlerMapping) xmlRpcServletServer.getHandlerMapping();
                    propertyHandlerMapping.removeHandler(type.getName());
                }

            }
        };
    }

    @Override
    protected <T> T doRefer(final Class<T> type, URL url) throws RpcException {
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        try {
            final XmlRpcClient client = new XmlRpcClient();
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setConnectionTimeout(timeout);
            config.setReplyTimeout(timeout);
            config.setEnabledForExtensions(true);
            config.setEnabledForExceptions(true);
            config.setServerURL(new java.net.URL("http://" + url.getHost() + ":" + url.getPort() + getContextPath(url)));
            client.setConfig(config);
            client.setMaxThreads(connections);
            ClientFactory clientFactory = new ClientFactory(client);
            return (T) clientFactory.newInstance(XmlProtocol.class.getClassLoader(),type,type.getName());
        } catch (MalformedURLException e) {
            throw new RpcException(e);
        }

    }

    private class XmlHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            RpcContext.getContext().setRemoteAddress(request.getRemoteHost(), request.getServerPort());
            XmlRpcServletServer xmlRpcServletServer = serverMap.get(request.getServerPort());
            xmlRpcServletServer.execute(request, response);
        }
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    private String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/");
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }
}
