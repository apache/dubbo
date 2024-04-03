package org.apache.dubbo.xds.listener;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.xds.protocol.XdsResourceListener;

import io.envoyproxy.envoy.config.listener.v3.Listener;

@SPI
public interface LdsListener extends XdsResourceListener<Listener> {

}
