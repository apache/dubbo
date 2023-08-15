package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.metadata.report.MetadataReportFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.List;

@Activate(order = -1000)
public class MetadataDestroyApplicationLifecycle implements ApplicationLifecycle{

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }

    @Override
    public void postDestroy(ApplicationContext applicationContext) {
        destroyMetadataReports(applicationContext.getModel());
    }

    private void destroyMetadataReports(ApplicationModel applicationModel) {
        // only destroy MetadataReport of this application
        List<MetadataReportFactory> metadataReportFactories = applicationModel.getExtensionLoader(MetadataReportFactory.class).getLoadedExtensionInstances();

        for (MetadataReportFactory metadataReportFactory : metadataReportFactories) {
            metadataReportFactory.destroy();
        }
    }

}
