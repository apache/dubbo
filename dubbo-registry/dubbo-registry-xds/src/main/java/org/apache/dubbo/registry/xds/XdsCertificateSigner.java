package org.apache.dubbo.registry.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

import javax.net.ssl.KeyManager;

@SPI
public interface XdsCertificateSigner {

    KeyManager request(URL url);
}
