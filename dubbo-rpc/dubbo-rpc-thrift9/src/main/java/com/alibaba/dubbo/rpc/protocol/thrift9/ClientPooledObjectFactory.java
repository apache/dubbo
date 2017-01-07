package com.alibaba.dubbo.rpc.protocol.thrift9;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.lang.reflect.Constructor;

/**
 * Created by wuyu on 2016/6/13.
 */
public class ClientPooledObjectFactory extends BasePooledObjectFactory<TServiceClient> {


    private int timeout = 1000;

    private String host = "localhost";

    private int port = 20990;

    private String serverName;

    public ClientPooledObjectFactory(String host, int port, int timeout, String serviceName) {
        this.host = host;
        this.timeout = timeout;
        this.port = port;
        this.serverName = serviceName;
    }

    @Override
    public TServiceClient create() throws Exception {
        // 使用高密度二进制协议
        TFramedTransport tFramedTransport = new TFramedTransport(new TSocket(host, port, timeout, timeout));
        TProtocol protocol = new TCompactProtocol(tFramedTransport);
        TMultiplexedProtocol tml = new TMultiplexedProtocol(protocol, serverName);
        String clientClsName = serverName.replace("$Iface", "") + "$Client";
        Class<?> clazz = Class.forName(clientClsName);
        Constructor constructor = clazz.getConstructor(TProtocol.class);
        tFramedTransport.open();
        return (TServiceClient) constructor.newInstance(tml);
    }

    @Override
    public PooledObject<TServiceClient> wrap(TServiceClient obj) {
        return new DefaultPooledObject<TServiceClient>(obj);
    }

    @Override
    public boolean validateObject(PooledObject<TServiceClient> p) {

        if (p.getObject() != null) {
            TServiceClient tServiceClient = p.getObject();
            TTransport outTransport = tServiceClient.getOutputProtocol().getTransport();
            TTransport inPutTransport = tServiceClient.getInputProtocol().getTransport();
            if (inPutTransport.isOpen() && outTransport.isOpen()) {
                return true;
            } else {
                try {
                    inPutTransport.open();
                    outTransport.open();
                    return true;
                } catch (TTransportException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public void destroyObject(PooledObject<TServiceClient> p) throws Exception {
        if (p.getObject() != null) {
            TServiceClient tServiceClient = p.getObject();
            TTransport outputProtocol = tServiceClient.getOutputProtocol().getTransport();
            TTransport inputProtocol = tServiceClient.getInputProtocol().getTransport();
            if (inputProtocol.isOpen()) {
                inputProtocol.flush();
                inputProtocol.close();
            }

            if (outputProtocol.isOpen()) {
                inputProtocol.flush();
                inputProtocol.close();
            }
        }
        p.markAbandoned();
    }


    @Override
    public void activateObject(PooledObject<TServiceClient> p) throws TTransportException {
        TServiceClient tServiceClient = p.getObject();
        TTransport outTransport = tServiceClient.getOutputProtocol().getTransport();
        TTransport inPutTransport = tServiceClient.getInputProtocol().getTransport();

        if(!inPutTransport.isOpen()){
            inPutTransport.open();
        }

        if (!outTransport.isOpen()) {
            outTransport.open();
        }


    }


}