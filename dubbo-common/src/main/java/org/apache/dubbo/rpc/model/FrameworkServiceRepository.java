package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.BaseServiceMetadata.interfaceFromServiceKey;
import static org.apache.dubbo.common.BaseServiceMetadata.versionFromServiceKey;

/**
 * Service repository for framework
 */
public class FrameworkServiceRepository {
    private FrameworkModel frameworkModel;

    // useful to find a provider model quickly with serviceInterfaceName:version
    private ConcurrentMap<String, ProviderModel> providersWithoutGroup = new ConcurrentHashMap<>();

    // useful to find a url quickly with serviceInterfaceName:version
    private ConcurrentMap<String, Set<URL>> providerUrlsWithoutGroup = new ConcurrentHashMap<>();

    public FrameworkServiceRepository(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    public void registerProvider(ProviderModel providerModel) {
        String key = keyWithoutGroup(providerModel.getServiceKey());
        ProviderModel previous = providersWithoutGroup.putIfAbsent(key, providerModel);
        if (previous != null && previous != providerModel) {
            throw new IllegalStateException("Register duplicate provider for key: " + key);
        }
    }

    public void unregisterProvider(ProviderModel providerModel) {
        String key = keyWithoutGroup(providerModel.getServiceKey());
        providersWithoutGroup.remove(key);
        providerUrlsWithoutGroup.remove(key);
    }

    public ProviderModel lookupExportedServiceWithoutGroup(String key) {
        return providersWithoutGroup.get(key);
    }

    public void registerProviderUrl(URL url) {
        providerUrlsWithoutGroup.computeIfAbsent(keyWithoutGroup(url.getServiceKey()), (k) -> new ConcurrentHashSet<>()).add(url);
    }

    public Set<URL> lookupRegisteredProviderUrlsWithoutGroup(String key) {
        return providerUrlsWithoutGroup.get(key);
    }

    public ServiceDescriptor lookupService(String interfaceName) {
        for (ApplicationModel applicationModel : frameworkModel.getApplicationModels()) {
            ServiceDescriptor serviceDescriptor = applicationModel.getApplicationServiceRepository().lookupService(interfaceName);
            if (serviceDescriptor != null) {
                return serviceDescriptor;
            }
        }
        return null;
    }

    private static String keyWithoutGroup(String serviceKey) {
        String interfaceName = interfaceFromServiceKey(serviceKey);
        String version = versionFromServiceKey(serviceKey);
        if (StringUtils.isEmpty(version)) {
            return interfaceName;
        }
        return interfaceName + ":" + version;
    }

}
