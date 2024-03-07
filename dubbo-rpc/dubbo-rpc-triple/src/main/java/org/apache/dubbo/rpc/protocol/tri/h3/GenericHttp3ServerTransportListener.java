package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http3.h3.Http3TransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.GenericHttp2ServerTransportListener;

public class GenericHttp3ServerTransportListener extends GenericHttp2ServerTransportListener
    implements Http3TransportListener {

    public GenericHttp3ServerTransportListener(
            H2StreamChannel h2StreamChannel,
            URL url,
            FrameworkModel frameworkModel) {
        super(h2StreamChannel, url, frameworkModel);
    }

    @Override
    public void onDataCompletion() {
        serverCallListener.onComplete();
    }
}
