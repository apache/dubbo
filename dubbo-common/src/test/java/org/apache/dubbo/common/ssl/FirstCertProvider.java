package org.apache.dubbo.common.ssl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Activate(order = -10000)
public class FirstCertProvider implements CertProvider {
    private static final AtomicBoolean isSupport = new AtomicBoolean(false);
    private static final AtomicReference<ProviderCert> providerCert = new AtomicReference<>();
    private static final AtomicReference<Cert> cert = new AtomicReference<>();
    @Override
    public boolean isSupport(URL address) {
        return isSupport.get();
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        return providerCert.get();
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        return cert.get();
    }

    public static void setSupport(boolean support) {
        isSupport.set(support);
    }

    public static void setProviderCert(ProviderCert providerCert) {
        FirstCertProvider.providerCert.set(providerCert);
    }

    public static void setCert(Cert cert) {
        FirstCertProvider.cert.set(cert);
    }
}
