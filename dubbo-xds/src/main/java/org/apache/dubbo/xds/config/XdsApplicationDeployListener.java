package org.apache.dubbo.xds.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.PilotExchanger;

import java.util.Collection;

import static org.apache.dubbo.rpc.Constants.SUPPORT_MESH_TYPE;

public class XdsApplicationDeployListener implements ApplicationDeployListener {
    @Override
    public void onInitialize(ApplicationModel scopeModel) {
        System.out.println("hello");
    }

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        Collection<RegistryConfig> registryConfigs = scopeModel.getApplicationConfigManager().getRegistries();
        for (RegistryConfig registryConfig : registryConfigs) {
            String protocol = registryConfig.getProtocol();
            if (StringUtils.isNotEmpty(protocol) && SUPPORT_MESH_TYPE.contains(protocol)) {
                URL url = URL.valueOf(registryConfig.getAddress());
                url.setScopeModel(scopeModel);
                scopeModel.getBeanFactory().registerBean(PilotExchanger.createInstance(url));
                break;
            }
        }
    }

    @Override
    public void onStarted(ApplicationModel scopeModel) {
        System.out.println("hello");
    }

    @Override
    public void onStopping(ApplicationModel scopeModel) {

    }

    @Override
    public void onStopped(ApplicationModel scopeModel) {

    }

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {

    }
}
