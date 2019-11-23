package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.metadata.WritableMetadataService;

import java.util.SortedSet;
import java.util.function.BiFunction;

/**
 * 2019-08-14
 *
 * @since 2.7.5
 */
public class RemoteWritableMetadataServiceDelegate implements WritableMetadataService {
    InMemoryWritableMetadataService defaultWritableMetadataService;
    RemoteWritableMetadataService remoteWritableMetadataService;

    public RemoteWritableMetadataServiceDelegate() {
        defaultWritableMetadataService = (InMemoryWritableMetadataService) WritableMetadataService.getExtension("local");
        remoteWritableMetadataService = new RemoteWritableMetadataService(defaultWritableMetadataService);
    }

    private WritableMetadataService getDefaultWritableMetadataService() {
        return defaultWritableMetadataService;
    }

    @Override
    public boolean exportURL(URL url) {
        return doFunction(WritableMetadataService::exportURL, url);
    }

    @Override
    public boolean unexportURL(URL url) {
        return doFunction(WritableMetadataService::unexportURL, url);
    }

    @Override
    public boolean subscribeURL(URL url) {
        return doFunction(WritableMetadataService::subscribeURL, url);
    }

    @Override
    public boolean unsubscribeURL(URL url) {
        return doFunction(WritableMetadataService::unsubscribeURL, url);
    }

    @Override
    public boolean refreshMetadata(String exportedRevision, String subscribedRevision) {
        boolean result = true;
        result &= defaultWritableMetadataService.refreshMetadata(exportedRevision, subscribedRevision);
        result &= remoteWritableMetadataService.refreshMetadata(exportedRevision, subscribedRevision);
        return result;
    }

    @Override
    public void publishServiceDefinition(URL providerUrl) {
        defaultWritableMetadataService.publishServiceDefinition(providerUrl);
        remoteWritableMetadataService.publishServiceDefinition(providerUrl);
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return getDefaultWritableMetadataService().getExportedURLs(serviceInterface, group, version, protocol);
    }

    @Override
    public SortedSet<String> getSubscribedURLs() {
        return getDefaultWritableMetadataService().getSubscribedURLs();
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return getDefaultWritableMetadataService().getServiceDefinition(interfaceName, version, group);
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return getDefaultWritableMetadataService().getServiceDefinition(serviceKey);
    }

    private boolean doFunction(BiFunction<WritableMetadataService, URL, Boolean> func, URL url) {
        return func.apply(defaultWritableMetadataService, url) && func.apply(remoteWritableMetadataService, url);
    }
}
