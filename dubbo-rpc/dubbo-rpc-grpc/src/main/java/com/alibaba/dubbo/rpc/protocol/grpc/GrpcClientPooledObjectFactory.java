package com.alibaba.dubbo.rpc.protocol.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.internal.ManagedChannelImpl;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * Created by wuyu on 2017/1/17.
 */
public class GrpcClientPooledObjectFactory extends BasePooledObjectFactory<Object> {

    private String host;

    private int port;

    private int timeout;

    private Class<?> clazz;

    private Field channelField = null;

    public GrpcClientPooledObjectFactory(String host, int port, int timeout, Class<?> clazz) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.clazz = clazz;
    }


    @Override
    public synchronized Object create() throws Exception {
        Constructor<?> stubConstructor = clazz.getDeclaredConstructor(Channel.class);
        stubConstructor.setAccessible(true);
        ManagedChannelImpl build = NettyChannelBuilder.forAddress(host, port)
                .idleTimeout(timeout, TimeUnit.MILLISECONDS)
                .negotiationType(NegotiationType.PLAINTEXT)
                .build();
        return stubConstructor.newInstance(build);
    }

    @Override
    public PooledObject<Object> wrap(Object obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public boolean validateObject(PooledObject<Object> p) {
        try {
            if (channelField == null) {
                channelField = p.getObject().getClass().getSuperclass().getDeclaredField("channel");
                channelField.setAccessible(true);

            }
            ManagedChannel managedChannel = (ManagedChannel) channelField.get(p.getObject());
            return !managedChannel.isShutdown() || !managedChannel.isTerminated();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void destroyObject(PooledObject<Object> p) throws Exception {
        try {
            if (channelField == null) {
                channelField = p.getObject().getClass().getSuperclass().getDeclaredField("channel");
                channelField.setAccessible(true);
            }
            ManagedChannel managedChannel = (ManagedChannel) channelField.get(p.getObject());
            managedChannel.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
