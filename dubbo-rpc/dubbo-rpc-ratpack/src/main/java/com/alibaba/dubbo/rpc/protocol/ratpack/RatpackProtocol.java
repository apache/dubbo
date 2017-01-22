//package com.alibaba.dubbo.rpc.protocol.ratpack;
//
//import com.alibaba.dubbo.common.Constants;
//import com.alibaba.dubbo.common.URL;
//import com.alibaba.dubbo.config.ProtocolConfig;
//import com.alibaba.dubbo.config.spring.ServiceBean;
//import com.alibaba.dubbo.rpc.RpcException;
//import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.core.io.Resource;
//import ratpack.func.Action;
//import ratpack.server.RatpackServer;
//import ratpack.server.RatpackServerSpec;
//import ratpack.server.internal.DefaultRatpackServer;
//import ratpack.server.internal.DefaultServerConfigBuilder;
//import ratpack.server.internal.ServerEnvironment;
//
//import java.io.IOException;
//import java.lang.reflect.Method;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Created by wuyu on 2017/1/18.
// */
//public class RatpackProtocol extends AbstractProxyProtocol {
//
//    private final Map<String, RatpackServer> serverMap = new ConcurrentHashMap<>();
//
//    private final Map<String, ChainConfigurers> chainConfigurersMap = new ConcurrentHashMap<>();
//
//    @Override
//    public int getDefaultPort() {
//        return 30000;
//    }
//
//    @Override
//    protected <T> Runnable doExport(final T impl, final Class<T> type, final URL url) throws RpcException {
//
//        final int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
//        final int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
//        final String addr = url.getHost() + ":" + url.getPort();
//
//        try {
//
//            RatpackServer ratpackServer = serverMap.get(addr);
//
//            if (ratpackServer == null) {
//                final ChainConfigurers chainConfigurers = new ChainConfigurers();
//
//                String[] serviceBeans = ServiceBean.getSpringContext().getBeanNamesForType(ServiceBean.class);
//                for (String beanName : serviceBeans) {
//                    ServiceBean bean = ServiceBean.getSpringContext().getBean(beanName, ServiceBean.class);
//                    List<ProtocolConfig> protocols = bean.getProtocols();
//                    for (ProtocolConfig protocolConfig : protocols) {
//                        if (url.getProtocol().equalsIgnoreCase(protocolConfig.getName())) {
//                            Object ref = bean.getRef();
//                            chainConfigurers.addAction(ref);
//                        }
//                    }
//                }
//                chainConfigurersMap.put(addr, chainConfigurers);
//                Action<? super RatpackServerSpec> spec = new Action<RatpackServerSpec>() {
//                    @Override
//                    public void execute(RatpackServerSpec ratpackServerSpec) throws Exception {
//                        ratpackServerSpec.handlers(chainConfigurers)
//                                .serverConfig(new DefaultServerConfigBuilder(
//                                        ServerEnvironment.env())
//                                        .threads(connections)
//                                        .connectTimeoutMillis(timeout)
//                                        .port(url.getPort())
//                                        .baseDir(initBaseDir().getFile().getAbsoluteFile()));
//                    }
//                };
//                DefaultRatpackServer server = new DefaultRatpackServer(spec);
//                server.start();
//            }
//
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        return new Runnable() {
//            @Override
//            public void run() {
//                chainConfigurersMap.get(addr).removeAction(type);
//            }
//        };
//    }
//
//    @Override
//    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
//        return null;
//    }
//
//    @Override
//    public void destroy() {
//        for (RatpackServer server : serverMap.values()) {
//            try {
//                server.stop();
//            } catch (Exception e) {
//
//            }
//        }
//        serverMap.clear();
//        chainConfigurersMap.clear();
//    }
//
//    static Resource initBaseDir() {
//        ClassPathResource classPath = new ClassPathResource("");
//        try {
//            if (classPath.getURL().toString().startsWith("jar:")) {
//                return classPath;
//            }
//        } catch (IOException e) {
//            // Ignore
//        }
//        FileSystemResource resources = new FileSystemResource("src/main/resources");
//        if (resources.exists()) {
//            return resources;
//        }
//        return new FileSystemResource(".");
//    }
//}
