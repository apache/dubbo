package com.alibaba.dubbo.rpc.protocol.springmvc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.springmvc.exception.SpringMvcErrorDecoder;
import com.alibaba.dubbo.rpc.protocol.springmvc.support.ApacheHttpClient;
import com.alibaba.dubbo.rpc.protocol.springmvc.support.SpringMvcFeign;
import com.alibaba.dubbo.rpc.protocol.springmvc.util.SpringUtil;
import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author wuyu DATA:2016-6-18
 */
public class SpringMvcProtocol extends AbstractProxyProtocol {

    private static final int DEFAULT_PORT = 8080;

    private final Map<String, SpringMvcHttpServer> servers = new ConcurrentHashMap<String, SpringMvcHttpServer>();

    private final SpringMvcServerFactory serverFactory = new SpringMvcServerFactory();

    //检测是否存在 springboot
    private static final boolean springboot =
            ClassUtils.isPresent("org.springframework.boot.autoconfigure.EnableAutoConfigurationImportSelector", AbstractProxyProtocol.class.getClassLoader());

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        serverFactory.setHttpBinder(httpBinder);
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {

        //检测当前是否存在springboot,如果存在 将不对外导出服务,由springboot提供相关rest服务
        //但是会注册提供服务的机器地址,dubbo-springmvc消费端 可以直接消费 springboot 提供的rest服务
        //如果 server 值为none,只注册服务,不导出服务,由第三方提供rest服务.dubbo消费 第三方rest服务
        if (springboot || url.getParameter(Constants.SERVER_KEY, "none").equals("none")) {
            return new Runnable() {
                public void run() {
                    //不做任何操作
                }
            };
        }

        final String addr = url.getHost() + ":" + url.getPort();
        SpringMvcHttpServer server = servers.get(addr);
        if (server == null) {
            server = serverFactory.createServer(url.getParameter(Constants.SERVER_KEY, "jetty"));
            server.start(url);
            servers.put(addr, server);
        }

        server.deploy(type);

        return new Runnable() {
            public void run() {
                servers.get(addr).undeploy(type);
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        int port = url.getPort();
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);

        String schema = "http://";
        if (port == 443 || port == 8433) {
            schema = "https://";
        }

        String api = schema + url.getHost() + ":" + url.getPort() + getContextPath(url);

        //注册请求拦截器,请求前认证
        Set<RequestInterceptor> requestInterceptors = SpringUtil.getBeans(RequestInterceptor.class);

        return SpringMvcFeign.builder()
                .requestInterceptors(requestInterceptors)
                .client(new ApacheHttpClient(SpringMvcFeign.getDefaultHttpClientPool(connections, timeout, 0, true)))
                .retryer(new Retryer.Default(100, SECONDS.toMillis(1), 0))
                .errorDecoder(new SpringMvcErrorDecoder())
                .target(type, api);

    }

    protected String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/");
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }

    protected int getErrorCode(Throwable e) {
        return super.getErrorCode(e);
    }

    public void destroy() {
        servers.clear();
        super.destroy();
    }

}
