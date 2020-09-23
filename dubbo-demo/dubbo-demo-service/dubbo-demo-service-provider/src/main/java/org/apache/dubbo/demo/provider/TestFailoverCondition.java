/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.store.failover.FailoverCondition;

public class TestFailoverCondition implements FailoverCondition {
    @Override
    public boolean shouldRegister(URL url) {
        return url.getPort() == 2182;
    }

    @Override
    public boolean shouldQuery(URL url) {
        return true;
    }

    @Override
    public boolean isLocalDataCenter(URL url) {
        return url.getPort() == 2182;
    }
}