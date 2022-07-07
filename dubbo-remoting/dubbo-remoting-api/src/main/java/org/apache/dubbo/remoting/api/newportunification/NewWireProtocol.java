package org.apache.dubbo.remoting.api.newportunification;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

@SPI(scope = ExtensionScope.FRAMEWORK)
public interface NewWireProtocol {
    NewProtocolDetector detector();

    void configServerPipeline(URL url, ChannelWithHandler ch);

    void configClientPipeline(URL url, ChannelWithHandler ch);

    void close();
}
