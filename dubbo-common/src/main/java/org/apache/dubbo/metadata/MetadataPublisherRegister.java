package org.apache.dubbo.metadata;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class MetadataPublisherRegister implements ApplicationDeployListener {

    @Override
    public void onStarting(ApplicationModel scopeModel) {
        ExtensionLoader<MetadataPublisher> metadataPublisherLoader = scopeModel.getExtensionLoader(MetadataPublisher.class);
        MetadataPublisher metadataPublisher = metadataPublisherLoader.getDefaultExtension();
        if(metadataPublisher != null){
            scopeModel.getBeanFactory().registerBean(metadataPublisher);
        }
    }
    
    @Override
    public void onInitialize(ApplicationModel scopeModel) {}

    @Override
    public void onStarted(ApplicationModel scopeModel) {}

    @Override
    public void onStopping(ApplicationModel scopeModel) {}

    @Override
    public void onStopped(ApplicationModel scopeModel) {}

    @Override
    public void onFailure(ApplicationModel scopeModel, Throwable cause) {}
}
