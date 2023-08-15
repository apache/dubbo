package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.Future;

@Activate(order = -1000)
public class ApplicationPostOfflineLifecycle implements ApplicationLifecycle{

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void preDestroy(ApplicationContext applicationContext) {
        ApplicationModel applicationModel = applicationContext.getModel();

        DeregisterApplicationLifecycle deregisterApplicationLifecycle = applicationModel.getBeanFactory().getBean(DeregisterApplicationLifecycle.class);
        Future<?> asyncMetadataFuture = null;

        if(deregisterApplicationLifecycle != null){
            asyncMetadataFuture =  applicationModel.getBeanFactory().getBean(RegisterApplicationLifecycle.class).getAsyncMetadataFuture();
        }
        if (asyncMetadataFuture != null) {
            asyncMetadataFuture.cancel(true);
        }
    }
}
