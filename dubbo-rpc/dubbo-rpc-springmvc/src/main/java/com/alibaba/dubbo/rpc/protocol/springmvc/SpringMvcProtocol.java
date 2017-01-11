package com.alibaba.dubbo.rpc.protocol.springmvc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.springmvc.annotation.Fallback;
import com.alibaba.dubbo.rpc.protocol.springmvc.exception.SpringMvcErrorDecoder;
import com.alibaba.dubbo.rpc.protocol.springmvc.oauth2.FeignOAuth2Interceptor;
import com.alibaba.dubbo.rpc.protocol.springmvc.oauth2.SpringMvcOAuth2Interceptor;
import com.alibaba.dubbo.rpc.protocol.springmvc.support.ApacheHttpClient;
import com.alibaba.dubbo.rpc.protocol.springmvc.support.AppInfo;
import com.alibaba.dubbo.rpc.protocol.springmvc.support.SpringMvcFeign;
import com.alibaba.dubbo.rpc.protocol.springmvc.util.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import feign.Client;
import feign.Feign;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.hystrix.HystrixFeign;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.netflix.config.ConfigurationManager.getConfigInstance;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author wuyu DATA:2016-6-18
 */

public class SpringMvcProtocol extends AbstractProxyProtocol {

    private static final int DEFAULT_PORT = 8080;

    private final Map<String, SpringMvcHttpServer> servers = new ConcurrentHashMap<String, SpringMvcHttpServer>();

    private final SpringMvcServerFactory serverFactory = new SpringMvcServerFactory();

    //用来存放springcloud 服务列表
    private final Map<URL, AppInfo> springcloudServerList = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    private final RestTemplate restTemplate = SpringMvcFeign.restTemplate(20, 3000);

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
                servers.get(addr).undeploy(type);
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        int port = url.getPort();
        String protocol = url.getProtocol();

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

        Fallback fallback = type.getAnnotation(Fallback.class);
        if (fallback != null && void.class != fallback.fallback() && ClassUtils.isPresent("feign.hystrix.HystrixFeign", SpringMvcProtocol.class.getClassLoader())) {
            T bean = SpringUtil.getBeanNamesForType(fallback.fallback()).size() > 0 ? (T) SpringUtil.getBean(fallback.fallback()) : (T) objectNewInstance(fallback.fallback());
            HystrixFeign.Builder hystrixBuilder = SpringMvcFeign.hystrixBuilder()
                    .requestInterceptors(requestInterceptors)
                    .retryer(new Retryer.Default(100, SECONDS.toMillis(1), 0))
                    .client(getClient(url));
            return bean != null ? hystrixBuilder.target(type, api, bean) : hystrixBuilder.target(type, api);
        } else {
            Feign.Builder builder = SpringMvcFeign.builder()
                    .requestInterceptors(requestInterceptors)
                    .retryer(new Retryer.Default(100, SECONDS.toMillis(1), 0))
                    .errorDecoder(new SpringMvcErrorDecoder())
                    .client(getClient(url));
            return builder.target(type, api);
        }

    }

    public AppInfo getAppUrl(URL url) {
        AppInfo appInfo = this.springcloudServerList.get(url);
        if (appInfo == null) {
            updateSpringCloudServerList(url);
        }
        return this.springcloudServerList.get(url);
    }

    public void updateSpringCloudServerList(URL url) {
        String protocol = url.getProtocol();
        String username = url.getUsername();
        String password = url.getPassword();
        String basic = "";
        if (username != null && password != null) {
            basic = username + ":" + password + "@";
        }
        String discoveryUrl = basic + url.getHost() + ":" + url.getPort() + "/" + url.getPath();
        String schema = "http://";
        String servers = "";
        String name = "";
        if ("eureka".equalsIgnoreCase(protocol) || "eurekas".equalsIgnoreCase(protocol)) {
            JSONObject json = restTemplate.getForObject("http://" + discoveryUrl, JSONObject.class);
            if (json != null) {
                JSONObject application = json.getJSONObject("application");
                name = application.getString("name");
                JSONArray instances = application.getJSONArray("instance");
                for (int i = 0; i < instances.size(); i++) {
                    JSONObject instance = instances.getJSONObject(i);
                    String homePageUrl = instance.getString("homePageUrl");
                    if (homePageUrl.contains("https://")) {
                        schema = homePageUrl.substring(0, 8);
                    }
                    String path = homePageUrl.replace("http://", "").replace("https://", "");
                    String host = path.substring(0, path.indexOf("/"));
                    servers += host + ",";
                }
                servers = servers.substring(0, servers.length() - 1);
            }
        }

    }

    public Client getClient(URL url) {
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        //如果是eureka 直接获取url服务列表
        if (ClassUtils.isPresent("org.apache.http.client.HttpClient", SpringMvcProtocol.class.getClassLoader())) {
            return new ApacheHttpClient(SpringMvcFeign.getDefaultHttpClientPool(connections, timeout, 0, true));
        } else {
            return new Client.Default(null, null);
        }
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
        scheduledExecutorService.shutdown();
        servers.clear();
        super.destroy();

    }

}
