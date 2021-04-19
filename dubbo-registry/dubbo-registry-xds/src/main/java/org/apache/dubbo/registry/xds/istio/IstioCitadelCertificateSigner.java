package org.apache.dubbo.registry.xds.istio;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.xds.XdsCertificateSigner;

/**
 * @author Albumen
 * @date on 2021/4/2
 */
public class IstioCitadelCertificateSigner implements XdsCertificateSigner {
    @Override
    public KeyPair request(URL url) {
        return null;
    }
}
