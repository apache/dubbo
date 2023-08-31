package org.apache.dubbo.metadata.report;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataPublisher;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_CREATE_INSTANCE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_LOAD_METADATA;

@Activate(value = "default")
public class DefaultMetadataPublisher implements MetadataPublisher {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultMetadataPublisher.class);

    @Override
    public void publishServiceDefinition(URL url, ServiceDescriptor serviceDescriptor, ApplicationModel applicationModel) {
        if (getMetadataReports(applicationModel).isEmpty()) {
            String msg = "Remote Metadata Report Server is not provided or unavailable, will stop registering service definition to remote center!";
            logger.warn(REGISTRY_FAILED_LOAD_METADATA, "", "", msg);
            return;
        }

        try {
            String side = url.getSide();
            if (PROVIDER_SIDE.equalsIgnoreCase(side)) {
                String serviceKey = url.getServiceKey();
                FullServiceDefinition serviceDefinition = serviceDescriptor.getFullServiceDefinition(serviceKey);

                if (StringUtils.isNotEmpty(serviceKey) && serviceDefinition != null) {
                    serviceDefinition.setParameters(url.getParameters());
                    for (Map.Entry<String, MetadataReport> entry : getMetadataReports(applicationModel).entrySet()) {
                        MetadataReport metadataReport = entry.getValue();
                        if (!metadataReport.shouldReportDefinition()) {
                            logger.info("Report of service definition is disabled for " + entry.getKey());
                            continue;
                        }
                        metadataReport.storeProviderMetadata(
                            new MetadataIdentifier(
                                url.getServiceInterface(),
                                url.getVersion() == null ? "" : url.getVersion(),
                                url.getGroup() == null ? "" : url.getGroup(),
                                PROVIDER_SIDE,
                                applicationModel.getApplicationName())
                            , serviceDefinition);
                    }
                }
            } else {
                for (Map.Entry<String, MetadataReport> entry : getMetadataReports(applicationModel).entrySet()) {
                    MetadataReport metadataReport = entry.getValue();
                    if (!metadataReport.shouldReportDefinition()) {
                        logger.info("Report of service definition is disabled for " + entry.getKey());
                        continue;
                    }
                    metadataReport.storeConsumerMetadata(
                        new MetadataIdentifier(
                            url.getServiceInterface(),
                            url.getVersion() == null ? "" : url.getVersion(),
                            url.getGroup() == null ? "" : url.getGroup(),
                            CONSUMER_SIDE,
                            applicationModel.getApplicationName()),
                        url.getParameters());
                }
            }
        } catch (Exception e) {
            //ignore error
            logger.error(REGISTRY_FAILED_CREATE_INSTANCE, "", "", "publish service definition metadata error.", e);
        }
    }

    private static Map<String, MetadataReport> getMetadataReports(ApplicationModel applicationModel) {
        return applicationModel.getBeanFactory().getBean(MetadataReportInstance.class).getMetadataReports(false);
    }
}
