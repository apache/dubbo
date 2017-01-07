package com.alibaba.dubbo.rpc.protocol.thrift9;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

/**
 * Created by wuyu on 2016/6/13.
 */
public class TProtocolPooledObjectFactory extends BasePooledObjectFactory<TProtocol> {


    private int timeout = 1000;

    private String host = "localhost";

    private int port = 20990;

    public TProtocolPooledObjectFactory(String host, int port, int timeout) {
        this.host = host;
        this.timeout = timeout;
        this.port = port;
    }

    @Override
    public TProtocol create() throws Exception {
        // 使用高密度二进制协议
        TFramedTransport tFramedTransport = new TFramedTransport(new TSocket(host, port, timeout, timeout));
        TProtocol protocol = new TCompactProtocol(tFramedTransport);
        return protocol;
    }

    @Override
    public PooledObject<TProtocol> wrap(TProtocol obj) {
        return new DefaultPooledObject<TProtocol>(obj);
    }

    @Override
    public boolean validateObject(PooledObject<TProtocol> p) {

        if (p.getObject() != null) {
            TTransport tt = p.getObject().getTransport();
            if (tt.isOpen()) {
                return true;
            } else {
                try {
                    tt.open();
                    return true;
                } catch (TTransportException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public void destroyObject(PooledObject<TProtocol> p) throws Exception {
        if (p.getObject() != null && p.getObject().getTransport().isOpen()) {
            p.getObject().getTransport().flush();
            p.getObject().getTransport().close();
        }
        p.markAbandoned();
    }


    @Override
    public void activateObject(PooledObject<TProtocol> p) throws TTransportException {
        if (!p.getObject().getTransport().isOpen()) {
            p.getObject().getTransport().open();
        }
    }


}