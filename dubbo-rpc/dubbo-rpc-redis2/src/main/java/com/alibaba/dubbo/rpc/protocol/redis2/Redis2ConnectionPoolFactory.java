package com.alibaba.dubbo.rpc.protocol.redis2;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import redis.clients.jedis.Protocol;

/**
 * Created by wuyu on 2017/2/9.
 */
public class Redis2ConnectionPoolFactory extends BasePooledObjectFactory<Redis2Connection> {

    private String host;

    private int port;

    private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;

    private int soTimeout = Protocol.DEFAULT_TIMEOUT;

    public Redis2ConnectionPoolFactory(String host, int port, int soTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
    }

    public Redis2ConnectionPoolFactory(String host, int port, int soTimeout, int connectionTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
    }

    @Override
    public synchronized Redis2Connection create() throws Exception {
        return new Redis2Connection(host, port, this.connectionTimeout, this.soTimeout);
    }

    @Override
    public PooledObject<Redis2Connection> wrap(Redis2Connection obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public boolean validateObject(PooledObject<Redis2Connection> p) {
        return p.getObject().isConnected();
    }

    @Override
    public void destroyObject(PooledObject<Redis2Connection> p) throws Exception {
        p.getObject().close();
    }

}
