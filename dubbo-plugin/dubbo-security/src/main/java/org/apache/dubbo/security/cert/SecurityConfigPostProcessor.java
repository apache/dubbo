package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.config.ConfigPostProcessor;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.FrameworkModel;

import static org.apache.dubbo.common.constants.CommonConstants.SSL_ENABLED_KEY;

public class SecurityConfigPostProcessor implements ConfigPostProcessor {
    private final FrameworkModel frameworkModel;

    public SecurityConfigPostProcessor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public URL portProcessServiceConfig(ServiceConfig serviceConfig, URL url) {
        AuthenticationGovernor governor = frameworkModel.getBeanFactory().getBean(AuthenticationGovernor.class);
        if (governor == null) {
            return url;
        }

        if (url.getParameter(SSL_ENABLED_KEY, false)) {
            return url;
        }

        AuthPolicy authPolicy = governor.getPortPolicy(url.getPort());
        if (authPolicy == null || authPolicy == AuthPolicy.NONE) {
            return url;
        }

        return url.addParameter(SSL_ENABLED_KEY, true);
    }
}
