package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Objects;

public class CertDeployerListener implements ApplicationDeployListener {
    private final DubboCertManager dubboCertManager;


    public CertDeployerListener(FrameworkModel frameworkModel) {
        dubboCertManager = frameworkModel.getBeanFactory().getBean(DubboCertManager.class);
    }

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        scopeModel.getApplicationConfigManager().getSsl().ifPresent(sslConfig -> {
            if (Objects.nonNull(sslConfig.getRemoteCAAddress()) &&
                Objects.nonNull(sslConfig.getRemoteCAPort())) {
                dubboCertManager.connect(sslConfig.getRemoteCAAddress(), sslConfig.getRemoteCAPort());
            }
        });
    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {
    }

    @Override
    public void onStopping(ApplicationModel scopeModel) {
        dubboCertManager.disConnect();
    }

    @Override
    public void onStopped(ApplicationModel scopeModel) {

    }

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {
        dubboCertManager.disConnect();
    }
}
