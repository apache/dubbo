package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http3.h3.Http3ServerTransportListenerFactory;
import org.apache.dubbo.remoting.http3.h3.Http3TransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class GenericHttp3ServerTransportListenerFactory implements Http3ServerTransportListenerFactory {
    public static final GenericHttp3ServerTransportListenerFactory INSTANCE = new GenericHttp3ServerTransportListenerFactory();

    @Override
    public Http3TransportListener newInstance(H2StreamChannel streamChannel, URL url, FrameworkModel frameworkModel) {
        return new GenericHttp3ServerTransportListener(streamChannel, url, frameworkModel);
    }

    @Override
    public boolean supportContentType(String contentType) {
        return true;
    }
}
