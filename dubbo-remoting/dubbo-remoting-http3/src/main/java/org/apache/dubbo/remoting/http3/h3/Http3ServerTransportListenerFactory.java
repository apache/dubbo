package org.apache.dubbo.remoting.http3.h3;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.rpc.model.FrameworkModel;

@SPI(scope = ExtensionScope.FRAMEWORK)
public interface Http3ServerTransportListenerFactory {
    Http3TransportListener newInstance(H2StreamChannel streamChannel, URL url, FrameworkModel frameworkModel);

    boolean supportContentType(String contentType);
}
