package org.apache.dubbo.metadata.store.failover;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metadata.report.MetadataReport;

/**
 * @author yiji@apache.org
 */
public abstract class StrategyMetadataReport implements MetadataReport {

    // failover configured url, eg: failover://127.0.1:2181?backup=localhost:2181|localhost:2181
    protected URL url;

    protected static final String STRATEGY_KEY = "strategy";

    // proxy metadata report strategy, used to decide whether to write or read metadata
    protected FailoverCondition strategy;

    protected ExtensionLoader<FailoverCondition> failoverLoader = ExtensionLoader.getExtensionLoader(FailoverCondition.class);

    public StrategyMetadataReport(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url is required.");
        }
        this.url = url;
        createFailoverStrategy(url);
    }

    protected void createFailoverStrategy(URL url) {
        String strategy = url.getParameter(STRATEGY_KEY);
        if (strategy != null) {
            if (!failoverLoader.hasExtension(strategy)) {
                throw new IllegalArgumentException("No '" + strategy + "' failover condition extension found.");
            }
            this.strategy = failoverLoader.getExtension(strategy);
        }
    }

    /**
     * Whether metadata should be reported.
     *
     * @param url registry url, eg: zookeeper://127.0.0.1:2181
     * @return true store metadata to the specified URL.
     */
    protected boolean shouldRegister(URL url) {
        return this.strategy == null ? true : this.strategy.shouldRegister(url);
    }

    /**
     * Whether metadata should be read from specified url.
     *
     * @param url registry url, eg: zookeeper://127.0.0.1:2181
     * @return true read metadata from specified URL.
     */
    protected boolean shouldQuery(URL url) {
        return this.strategy == null ? true : this.strategy.shouldQuery(url);
    }

    /**
     * Judge whether it is a local region or a local datacenter.
     * <p>
     * Allows the local region or datacenter to be read first.
     *
     * @param url
     * @return
     */
    protected boolean isLocalDataCenter(URL url) {
        return this.strategy == null ? true : this.strategy.isLocalDataCenter(url);
    }

}