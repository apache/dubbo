package org.apache.dubbo.rpc.protocol.mvc.annotation.consumer;

import org.apache.dubbo.common.extension.SPI;

@SPI()
public interface HttpConnectionPreBuildIntercept {
    void intercept(HttpConnectionCreateContext connectionCreateContext);
}
