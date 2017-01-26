package com.alibaba.dubbo.rpc.protocol.thrift9;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.proxy.JdkProxySource;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;

import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by wuyu on 2016/6/12.
 */
public class Thrift9Protocol extends AbstractProxyProtocol {

    public static final int DEFAULT_PORT = 20990;

    private final Map<String, TThreadPoolServer> serversMap = new ConcurrentHashMap<String, TThreadPoolServer>();

    private final Map<String, TMultiplexedProcessor> processorMap = new ConcurrentHashMap<>();

    private final Map<String, GenericObjectPool> poolMap = new ConcurrentHashMap<>();

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {

        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int port = url.getPort();
        final String addr = url.getIp() + ":" + url.getPort();

        TThreadPoolServer server = serversMap.get(addr);
        final String IfaceName = type.getName();

        try {
            // 获取Iface接口
            Class<?> iFace = Class.forName(type.getName());

            // 获取processor构造方法
            Constructor<?> constructor = Class.forName(IfaceName.replace("$Iface", "") + "$Processor")
                    .getConstructor(iFace);

            TProcessor serviceProcessor = (TProcessor) constructor.newInstance(impl);

            if (server == null) {
                TServerTransport transport = transport = new TServerSocket(port);

                // 多线程处理器参数设置
                TThreadPoolServer.Args args = new TThreadPoolServer.Args(transport);

                // 用户请求完成后,出站口
                args.outputTransportFactory(new TFramedTransport.Factory());
                args.inputTransportFactory(new TFramedTransport.Factory());

                // 设置高压缩二进制协议
                args.protocolFactory(new TCompactProtocol.Factory());

                // 服务管理器
                TMultiplexedProcessor processor = new TMultiplexedProcessor();

                processorMap.put(addr, processor);

                args.processor(processor);
                // 设置线程池初始化数量
                args.minWorkerThreads(10);
                // 最大允许并发,超过并发,线程阻塞.排队处理
                args.maxWorkerThreads(threads);
                args.requestTimeoutUnit(TimeUnit.MILLISECONDS);
                args.requestTimeout(timeout);

                // 多线程服务 //默认初始化5个线程
                server = new TThreadPoolServer(args);

                serversMap.put(addr, server);
                // 设置监听器
                // server.setServerEventHandler(new ServerEven());

                processor.registerProcessor(type.getName(), serviceProcessor);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 启动服务
                        serversMap.get(addr).serve();
                    }
                }).start();

            } else {

                TMultiplexedProcessor processor = processorMap.get(addr);

                // 注册服务
                processor.registerProcessor(type.getName(), serviceProcessor);
            }

        } catch (Exception e) {
            throw new RpcException(e);
        }

        return new Runnable() {
            @Override
            public void run() {
                TMultiplexedProcessor tMultiplexedProcessor = processorMap.get(addr);
                try {
                    unRegisterProcessor(tMultiplexedProcessor, IfaceName);
                } catch (Exception e) {
                    logger.warn("unRegsiter " + IfaceName + " faild!");
                }
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        final String addr = url.getIp() + ":" + url.getPort();
        final int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        ClientPooledObjectFactory factory = new ClientPooledObjectFactory(url.getHost(), url.getPort(), timeout, type.getName());
        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(threads);
        config.setMaxIdle(5);
        config.setBlockWhenExhausted(true);
        config.setTestOnReturn(true);
        config.setMaxWaitMillis(timeout);
        config.setTestWhileIdle(true);
        config.setMinEvictableIdleTimeMillis(300000);
        config.setTimeBetweenEvictionRunsMillis(60000);
        final GenericObjectPool<TServiceClient> pool = new GenericObjectPool<TServiceClient>(factory, config);
        poolMap.put(addr, pool);

        return (T) Proxy.newProxyInstance(Thrift9Protocol.class.getClassLoader(), new Class[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                TServiceClient obj = pool.borrowObject(timeout);
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
        super.destroy();
        for (String key : poolMap.keySet()) {
            GenericObjectPool pool = poolMap.get(key);
            pool.clear();
            pool.close();
        }
        for (String key : serversMap.keySet()) {
            processorMap.remove(key);
            TThreadPoolServer tThreadPoolServer = this.serversMap.remove(key);

            try {
                if (tThreadPoolServer.isServing()) {
                    tThreadPoolServer.stop();
                }
            } catch (Exception e) {
                logger.warn("server is not running !");
            }

        }


    }

    private void unRegisterProcessor(TMultiplexedProcessor tMultiplexedProcessor, String iFace)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        Field SERVICE_PROCESSOR_MAP = TMultiplexedProcessor.class.getField("SERVICE_PROCESSOR_MAP");
        Map map = (Map) SERVICE_PROCESSOR_MAP.get(tMultiplexedProcessor);
        map.remove(iFace);
    }

}