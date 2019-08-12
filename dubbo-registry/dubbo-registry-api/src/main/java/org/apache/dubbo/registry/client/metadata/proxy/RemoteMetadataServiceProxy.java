package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;

/**
 * 2019-08-09
 */
public class RemoteMetadataServiceProxy implements MetadataService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String serviceName;


    public RemoteMetadataServiceProxy(ServiceInstance serviceInstance) {
        this.serviceName = serviceInstance.getServiceName();
    }

    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return toSortedStrings(getMetadataReport().getSubscribedURLs());
    }

    // TODO, protocol should be used
    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return toSortedStrings(getMetadataReport().getExportedURLs(new MetadataIdentifier(serviceInterface, group, version, null, null)));
    }

    private static SortedSet<String> toSortedStrings(Collection<String> values) {
        return Collections.unmodifiableSortedSet(new TreeSet<>(values));
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return getMetadataReport().getServiceDefinition(new MetadataIdentifier(interfaceName,
                version, group, PROVIDER_SIDE, serviceName));
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        String[] services = UrlUtils.parseServiceKey(serviceKey);
        return getMetadataReport().getServiceDefinition(new MetadataIdentifier(services[1],
                services[0], services[2], PROVIDER_SIDE, serviceName));
    }

    MetadataReport getMetadataReport() {
        return MetadataReportInstance.getMetadataReport(true);
    }


}
