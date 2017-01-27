package com.alibaba.dubbo.rpc.protocol.grpc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.ServerImpl;
import io.grpc.netty.NettyServerBuilder;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.proxy.JdkProxySource;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/1/13.
 */
public class GrpcProtool extends AbstractProxyProtocol {

    private static final int DEFAULT_PORT = 40880;

    private Map<String, ServerImpl> serverMap = new ConcurrentHashMap<>();

    private Map<String, GenericObjectPool> poolMap = new ConcurrentHashMap<>();


    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        int port = url.getPort();
        String addr = url.getHost() + ":" + port;

        if (serverMap.get(addr) != null) {
            return new Runnable() {
                @Override
                public void run() {

                }
            };
        }

        String bindHost = url.getHost();
        if (url.getParameter("anyhost", false)) {
            bindHost = "0.0.0.0";
        }

        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        //gpc 并未提供动态插拔服务,暂时能想到一种方案进行解决
        //1.定义全局事件,等待所有Dubbo export service完成,一次性添加所有服务 并启动。
        NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress(bindHost, port))
                .maxConcurrentCallsPerConnection(threads);


        String[] serviceBeans = ServiceBean.getSpringContext().getBeanNamesForType(ServiceBean.class);
        for (String beanName : serviceBeans) {
            ServiceBean bean = ServiceBean.getSpringContext().getBean(beanName, ServiceBean.class);
            List<ProtocolConfig> protocols = bean.getProtocols();
            for (ProtocolConfig protocolConfig : protocols) {
                if (url.getProtocol().equalsIgnoreCase(protocolConfig.getName())) {
                    Object ref = bean.getRef();

                    try {
                        Method bindService = ref.getClass().getMethod("bindService");
                        final Object serviceDefine = bindService.invoke(ref);
                        builder.addService(new BindableService() {
                            @Override
                            public ServerServiceDefinition bindService() {
                                return (ServerServiceDefinition) serviceDefine;
                            }
                        });
                    } catch (Exception e) {
                        throw new RpcException(e);
                    }
                }
            }
        }
        ServerImpl serverImpl = builder.build();
        serverMap.put(addr, serverImpl);
        try {
            serverImpl.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return new Runnable() {
            @Override
            public void run() {

            }
        };
    }

    @Override
    protected <T> T doRefer(final Class<T> type, final URL url) throws RpcException {
        String host = url.getHost();
        int port = url.getPort();
        final int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);


        String name = type.getName().substring(0, type.getName().lastIndexOf("$"));
        final GenericObjectPool pool;
        final JdkProxySource clientProxy;
        try {

            Class<?> blockingStub = Class.forName(name + "$" + type.getSimpleName() + "Stub");
            GrpcClientPooledObjectFactory factory = new GrpcClientPooledObjectFactory(url.getHost(), url.getPort(), timeout, blockingStub);
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(threads);
            config.setMaxIdle(threads);
            config.setBlockWhenExhausted(true);
            config.setTestOnReturn(true);
            config.setMaxWaitMillis(timeout);
            config.setTestWhileIdle(true);
            pool = new GenericObjectPool<>(factory, config);

            poolMap.put(host + ":" + port, pool);
        } catch (Exception e) {
            throw new RpcException(e);
        }

        return (T) Proxy.newProxyInstance(GrpcProtool.class.getClassLoader(), new Class<?>[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object obj = pool.borrowObject(timeout);
                try {
                    return method.invoke(obj, args);
                } finally {
                    pool.returnObject(obj);
                }
            }
        });

    }

    @Override
    public void destroy() {
        for (GenericObjectPool pool : poolMap.values()) {
            try {
                pool.clear();
                pool.close();
            } catch (Exception e) {

            }
        }
    }
}
