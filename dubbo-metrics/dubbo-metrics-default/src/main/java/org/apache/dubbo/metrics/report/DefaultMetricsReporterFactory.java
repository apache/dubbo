package org.apache.dubbo.metrics.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class DefaultMetricsReporterFactory extends AbstractMetricsReporterFactory{
    private final ApplicationModel applicationModel;
    public DefaultMetricsReporterFactory(ApplicationModel applicationModel) {
        super(applicationModel);
        this.applicationModel = applicationModel;
    }
    
    @Override
    public MetricsReporter createMetricsReporter(URL url) {
        return new DefaultMetricsReporter(url, applicationModel);
    }
}
