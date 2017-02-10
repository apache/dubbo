package com.alibaba.dubbo.rpc.protocol.redis2;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/1/26.
 */
public class Redis2Protocol extends AbstractProxyProtocol {

    private Map<String, Redis2Server> serverMap = new ConcurrentHashMap<>();

    private List<Redis2Client> pools = new ArrayList<>();


    @Override
    public int getDefaultPort() {
        return 6380;
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {
        int port = url.getPort();
        String host = url.getHost();
        String addr = host + ":" + port;

        int connectTimeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT);
        int connections = url.getParameter(Constants.THREADPOOL_KEY, 200);
        Redis2Server redis2Server = serverMap.get(addr);
        if (redis2Server == null) {
            redis2Server = new Redis2Server(url.getParameter("anyhost", false) ? "0.0.0.0" : url.getHost(), port, connections, connectTimeout);
            redis2Server.start();
            serverMap.put(addr, redis2Server);
        }

        final RpcRedisCommandHandler rpcRedisCommandHandler = redis2Server.getRpcRedisCommandHandler();
        rpcRedisCommandHandler.addHandler(url.toFullString(), type, impl);

        return new Runnable() {
            @Override
            public void run() {
                rpcRedisCommandHandler.removeHandler(type);
            }
        };
    }

    @Override
    protected <T> T doRefer(final Class<T> type, URL url) throws RpcException {
        final int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        final int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        final int port = url.getPort();
        final String host = url.getHost();
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(connections);
        config.setMaxIdle(connections);
        config.setBlockWhenExhausted(true);
        config.setTestOnReturn(true);
        config.setMaxWaitMillis(timeout);
        config.setTestWhileIdle(true);
        Redis2ConnectionPoolFactory redis2ConnectionPoolFactory = new Redis2ConnectionPoolFactory(host, port, timeout);
        final Redis2Client redis2Client = new Redis2Client(redis2ConnectionPoolFactory, config);
        return (T) Proxy.newProxyInstance(Redis2Protocol.class.getClassLoader(), new Class[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return redis2Client.invoke(type, method, args);
            }
        });
    }

    @Override
    public void destroy() {
        for (Redis2Server redis2Server : serverMap.values()) {
            try {
                redis2Server.stop();
            } catch (Exception e) {

            }
        }
        for (Redis2Client pool : pools) {
            try {
                pool.close();
            } catch (Exception e) {

            }
        }


    }
}
