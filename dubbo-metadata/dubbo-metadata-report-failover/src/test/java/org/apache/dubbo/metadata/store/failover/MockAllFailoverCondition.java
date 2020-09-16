package org.apache.dubbo.metadata.store.failover;

import org.apache.dubbo.common.URL;

public class MockAllFailoverCondition extends MockLocalFailoverCondition {

    @Override
    public boolean shouldRegister(URL url) {
        return true;
    }

    @Override
    public boolean isLocalDataCenter(URL url) {
        // we don't care about local datacenter first.
        return false;
    }
}