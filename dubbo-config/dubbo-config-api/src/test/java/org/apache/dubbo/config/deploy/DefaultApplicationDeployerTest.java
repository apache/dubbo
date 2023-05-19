package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.utils.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultApplicationDeployerTest {

    @Test
    void isSupportPrometheus() {
        boolean supportPrometheus = new DefaultApplicationDeployer(null).isSupportPrometheus();
        Assert.assertTrue(supportPrometheus,"DefaultApplicationDeployer.isSupportPrometheus() should return true");
    }
}
