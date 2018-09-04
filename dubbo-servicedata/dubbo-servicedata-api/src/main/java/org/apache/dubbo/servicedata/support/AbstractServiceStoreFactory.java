package org.apache.dubbo.servicedata.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.servicedata.ServiceStore;
import org.apache.dubbo.servicedata.ServiceStoreFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author cvictory ON 2018/8/24
 */
public abstract class AbstractServiceStoreFactory implements ServiceStoreFactory {

    // Log output
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceStoreFactory.class);

    // The lock for the acquisition process of the registry
    private static final ReentrantLock LOCK = new ReentrantLock();

    // Registry Collection Map<RegistryAddress, Registry>
    private static final Map<String, ServiceStore> SERVICE_STORE_MAP = new ConcurrentHashMap<String, ServiceStore>();

    /**
     * Get all registries
     *
     * @return all registries
     */
    public static Collection<ServiceStore> getServiceStores() {
        return Collections.unmodifiableCollection(SERVICE_STORE_MAP.values());
    }

    @Override
    public ServiceStore getServiceStore(URL url) {
        url = url.setPath(ServiceStore.class.getName())
                .addParameter(Constants.INTERFACE_KEY, ServiceStore.class.getName())
                .removeParameters(Constants.EXPORT_KEY, Constants.REFER_KEY);
        String key = url.toServiceString();
        // Lock the registry access process to ensure a single instance of the registry
        LOCK.lock();
        try {
            ServiceStore serviceStore = SERVICE_STORE_MAP.get(key);
            if (serviceStore != null) {
                return serviceStore;
            }
            serviceStore = createServiceStore(url);
            if (serviceStore == null) {
                throw new IllegalStateException("Can not create servicestore " + url);
            }
            SERVICE_STORE_MAP.put(key, serviceStore);
            return serviceStore;
        } finally {
            // Release the lock
            LOCK.unlock();
        }
    }

    protected abstract ServiceStore createServiceStore(URL url);
}
