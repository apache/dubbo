package org.apache.dubbo.registry.integration;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.dubbo.common.Constants.DEFAULT_REGISTER_PROVIDER_KEYS;

/**
 * @author cvictory ON 2018/11/14
 */
public class RegistryProtocolTest {

    @Test
    public void testGetParamsToRegistry() {
        RegistryProtocol registryProtocol = new RegistryProtocol();
        String[] additionalParams = new String[]{"key1", "key2"};
        String[] registryParams = registryProtocol.getParamsToRegistry(DEFAULT_REGISTER_PROVIDER_KEYS, additionalParams);
        String[] expectParams = ArrayUtils.addAll(DEFAULT_REGISTER_PROVIDER_KEYS, additionalParams);
        Assert.assertArrayEquals(expectParams, registryParams);
    }
}

