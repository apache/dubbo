package com.alibaba.dubbo.rpc.protocol.springmvc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.rpc.FallBack;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.springmvc.oauth2.FeignOAuth2Interceptor;
import com.alibaba.dubbo.rpc.protocol.springmvc.oauth2.SpringMvcOAuth2Interceptor;
import com.alibaba.dubbo.rpc.protocol.springmvc.support.SpringMvcFeign;
import com.alibaba.dubbo.rpc.protocol.springmvc.util.SpringUtil;
import feign.RequestInterceptor;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        if (springboot || url.getParameter(Constants.SERVER_KEY, "").equals("none")) {
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

        // token 有值 自动开启filter
        String role = url.getParameter("token");

        //判断是否包含OAuth2Filter
        if (role != null) {
            //权限字段
            Set<String> beanNames = SpringUtil.getBeanNamesForType(SpringMvcOAuth2Interceptor.class);
            if (beanNames.size() > 0) {
                SpringMvcOAuth2Interceptor bean = SpringUtil.getBean(SpringMvcOAuth2Interceptor.class);
                bean.setEnable(true);
                bean.addRole(url.getServiceInterface(), role);
            }

        }

        server.deploy(type);

        return new Runnable() {
            public void run() {
                SpringMvcHttpServer server = servers.get(addr);
                if (server != null) {
                    server.undeploy(type);
                }
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        int port = url.getPort();
        String protocol = url.getProtocol();
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        String schema = "http://";
        if (port == 443 || port == 8433) {
            schema = "https://";
        }

        String api = schema + url.getHost() + ":" + url.getPort() + getContextPath(url);

        String addr = url.getHost() + ":" + url.getPort();


        //注册请求拦截器,请求前认证
        Set<RequestInterceptor> requestInterceptors = SpringUtil.getBeans(RequestInterceptor.class);
        String filter = url.getParameter("reference.filter");

        //判断是否包含OAuth2Filter
        if (filter != null && filter.contains("oAuth2Filter")) {
            requestInterceptors.add(new FeignOAuth2Interceptor());
        }

        FallBack apiAno = type.getAnnotation(FallBack.class);
        if (apiAno != null && void.class != apiAno.value() && ClassUtils.isPresent("feign.hystrix.HystrixFeign", SpringMvcProtocol.class.getClassLoader())) {
            T bean = SpringUtil.getBeanNamesForType(apiAno.value()).size() > 0 ? (T) SpringUtil.getBean(apiAno.value()) : (T) objectNewInstance(apiAno.value());
            return SpringMvcFeign.hystrixTarget(type, api, bean, connections, timeout, requestInterceptors);
        }

        return SpringMvcFeign.target(type, api, connections, timeout, requestInterceptors);

    }


    private <T> T objectNewInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/");
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }

    protected int getErrorCode(Throwable e) {
        return super.getErrorCode(e);
    }

    public void destroy() {
        Collection<SpringMvcHttpServer> values = servers.values();
        for (SpringMvcHttpServer server : values) {
            try {
                server.stop();
            } catch (Exception e) {

            }
        }
        servers.clear();
    }

}
