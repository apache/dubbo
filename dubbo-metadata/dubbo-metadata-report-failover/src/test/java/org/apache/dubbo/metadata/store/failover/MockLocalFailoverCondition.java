package org.apache.dubbo.metadata.store.failover;

import org.apache.dubbo.common.URL;

/**
 * @author yiji@apache.org
 */
public class MockLocalFailoverCondition implements FailoverCondition {

    @Override
    public boolean shouldRegister(URL url) {
        // we just register same datacenter.
        return isLocalDataCenter(url);
    }

    @Override
    public boolean shouldQuery(URL url) {
        // we want read any metadata report server.
        return true;
    }

    @Override
    public boolean isLocalDataCenter(URL url) {
        // we mock current datacenter is `127.0.0.1:2181`
        String current = "127.0.0.1:2181";
        return url.getBackupAddress().contains(current);
    }

}