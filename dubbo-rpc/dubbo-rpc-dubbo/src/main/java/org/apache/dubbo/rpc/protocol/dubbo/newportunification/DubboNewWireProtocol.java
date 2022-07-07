package org.apache.dubbo.rpc.protocol.dubbo.newportunification;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.newportunification.ChannelWithHandler;
import org.apache.dubbo.remoting.api.newportunification.NewProtocolDetector;
import org.apache.dubbo.remoting.api.newportunification.NewWireProtocol;

public class DubboNewWireProtocol implements NewWireProtocol {
    private final NewProtocolDetector detector = new DubboNewDetector();
    @Override
    public NewProtocolDetector detector() {
        return detector;
    }

    @Override
    public void configServerPipeline(URL url, ChannelWithHandler ch) {
        // config url and channelHandler for channel
        ch.setUrl(url);
        // here channel remove wrapper of its handler
        ch.setHandler(ch.getChannelHandler());
    }

    @Override
    public void configClientPipeline(URL url, ChannelWithHandler ch) {

    }

    @Override
    public void close() {

    }
}
