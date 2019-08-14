package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.WritableMetadataService;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.function.BiFunction;

/**
 * 2019-08-14
 *
 * @since 2.7.5
 */
public class RemoteWritableMetadataServiceDelegate implements WritableMetadataService {
    List<WritableMetadataService> metadataServiceList = new ArrayList<>(2);

    public RemoteWritableMetadataServiceDelegate() {
        metadataServiceList.add(WritableMetadataService.getDefaultExtension());
        metadataServiceList.add(new RemoteWritableMetadataService());
    }

    private WritableMetadataService getInMemoryWritableMetadataService() {
        for (WritableMetadataService writableMetadataService : metadataServiceList) {
            if (writableMetadataService instanceof InMemoryWritableMetadataService) {
                return writableMetadataService;
            }
        }
        return metadataServiceList.get(0);
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
        for (WritableMetadataService writableMetadataService : metadataServiceList) {
            result &= writableMetadataService.refreshMetadata(exportedRevision, subscribedRevision);
        }
        return result;
    }

    @Override
    public void publishServiceDefinition(URL providerUrl) {
        for (WritableMetadataService writableMetadataService : metadataServiceList) {
            writableMetadataService.publishServiceDefinition(providerUrl);
        }
    }

    @Override
    public SortedSet<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return getInMemoryWritableMetadataService().getExportedURLs(serviceInterface, group, version, protocol);
    }
    @Override
    public SortedSet<String> getSubscribedURLs() {
        return getInMemoryWritableMetadataService().getSubscribedURLs();
    }

    @Override
    public String getServiceDefinition(String interfaceName, String version, String group) {
        return getInMemoryWritableMetadataService().getServiceDefinition(interfaceName, version, group);
    }

    @Override
    public String getServiceDefinition(String serviceKey) {
        return getInMemoryWritableMetadataService().getServiceDefinition(serviceKey);
    }

    private boolean doFunction(BiFunction<WritableMetadataService, URL, Boolean> func, URL url) {
        boolean result = true;
        for (WritableMetadataService writableMetadataService : metadataServiceList) {
            result &= func.apply(writableMetadataService, url);
        }
        return result;
    }
}
