package org.apache.dubbo.metadata.store;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.MetadataService;

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

/**
 * @author cvictory ON 2019-08-14
 */
public class BaseWritableMetadataService {
    final Logger logger = LoggerFactory.getLogger(getClass());

    // =================================== Registration =================================== //

    /**
     * All exported {@link URL urls} {@link Map} whose key is the return value of {@link URL#getServiceKey()} method
     * and value is the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    static ConcurrentNavigableMap<String, SortedSet<URL>> exportedServiceURLs = new ConcurrentSkipListMap<>();

    // ==================================================================================== //

    // =================================== Subscription =================================== //

    /**
     * The subscribed {@link URL urls} {@link Map} of {@link MetadataService},
     * whose key is the return value of {@link URL#getServiceKey()} method and value is
     * the {@link SortedSet sorted set} of the {@link URL URLs}
     */
    final static ConcurrentNavigableMap<String, SortedSet<URL>> SUBSCRIBED_SERVICE_URLS = new ConcurrentSkipListMap<>();

    final static ConcurrentNavigableMap<String, String> SERVICE_DEFINITIONS = new ConcurrentSkipListMap<>();


    boolean throwableAction(Consumer<URL> consumer, URL url) {
        try {
            consumer.accept(url);
        } catch (Exception e) {
            logger.error("Failed to remove url metadata to remote center, url is: " + url);
            return false;
        }
        return true;
    }

    public SortedSet<String> getSubscribedURLs() {
        return getAllUnmodifiableServiceURLs(SUBSCRIBED_SERVICE_URLS);
    }

    static SortedSet<String> getAllUnmodifiableServiceURLs(Map<String, SortedSet<URL>> serviceURLs) {
        return MetadataService.toSortedStrings(serviceURLs.values().stream().flatMap(Collection::stream));
    }
}
