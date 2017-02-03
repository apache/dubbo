package com.alibaba.dubbo.rpc.protocol.jersey;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import io.netty.channel.Channel;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.springframework.util.ClassUtils;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/1/15.
 */

public class JerseyProtocol extends AbstractProxyProtocol {


    private Map<String, Channel> serverMap = new ConcurrentHashMap<>();

    private final List<ResteasyClient> clients = Collections.synchronizedList(new LinkedList<ResteasyClient>());


    @Override
    public int getDefaultPort() {
        return 8081;
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {
        final String addr = url.getHost() + ":" + url.getPort();

        if (serverMap.get(addr) != null) {
            return new Runnable() {
                @Override
                public void run() {

                }
            };
        }

        int port = url.getPort();
        String schema = "http://";
        if (port == 443 || port == 8433) {
            schema = "https://";
        }
        ResourceConfig resourceConfig = new ResourceConfig();
        if (ClassUtils.isPresent("org.glassfish.jersey.jackson.JacksonFeature", JerseyProtocol.class.getClassLoader())) {
            resourceConfig.register(JacksonFeature.class);
        }
        if (ClassUtils.isPresent("org.glassfish.jersey.moxy.xml.MoxyXmlFeature", JerseyProtocol.class.getClassLoader())) {
            resourceConfig.register(MoxyXmlFeature.class);
        }
        String[] serviceBeans = ServiceBean.getSpringContext().getBeanNamesForType(ServiceBean.class);
        for (String beanName : serviceBeans) {
            ServiceBean bean = ServiceBean.getSpringContext().getBean(beanName, ServiceBean.class);
            List<ProtocolConfig> protocols = bean.getProtocols();
            for (ProtocolConfig protocolConfig : protocols) {
                if (url.getProtocol().equalsIgnoreCase(protocolConfig.getName())) {
                    resourceConfig.registerInstances(bean.getRef());
                }
            }
        }


        final Channel http2Server = NettyHttpContainerProvider.createHttp2Server(URI.create(schema + addr + "/"), resourceConfig, null);
        serverMap.put(addr, http2Server);
        return new Runnable() {

            @Override
            public void run() {

            }
        };

    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        int port = url.getPort();
        String schema = "http://";
        if (port == 443 || port == 8433) {
            schema = "https://";
        }
        HttpClient httpClient = getDefaultHttpClientPool(connections, timeout, 0, true);
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient/*, localContext*/);
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
        if (ClassUtils.isPresent("org.glassfish.jersey.jackson.JacksonFeature", JerseyProtocol.class.getClassLoader())) {
            client.register(JacksonFeature.class);
        }
        if (ClassUtils.isPresent("org.glassfish.jersey.moxy.xml.MoxyXmlFeature", JerseyProtocol.class.getClassLoader())) {
            client.register(MoxyXmlFeature.class);
        }
        clients.add(client);
        ResteasyWebTarget target = client.target(schema + url.getHost() + ":" + url.getPort() + "/" + getContextPath(url));
        return target.proxy(type);
    }

    public static HttpClient getDefaultHttpClientPool(int maxTotal, int timeout, int retry, boolean keepAlive) {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(maxTotal)
                .setMaxConnTotal(maxTotal)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(retry, true));
        if (keepAlive) {
            httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
        }
        return httpClient.build();
    }

    @Override
    public void destroy() {
        Collection<Channel> values = serverMap.values();
        for (Channel channel : values) {
            try {
                channel.close();
            } catch (Exception e) {

            }
        }

        for (ResteasyClient client : clients) {
            try {
                client.close();
            } catch (Exception e) {

            }
        }
    }

    protected String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/");
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }
}
