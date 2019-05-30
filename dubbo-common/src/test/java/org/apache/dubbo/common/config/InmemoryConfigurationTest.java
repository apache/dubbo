package org.apache.dubbo.common.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
class InmemoryConfigurationTest {

    private static InmemoryConfiguration memConfig;
    private static final String MOCK_KEY = "mockKey";
    private static final String MOCK_VALUE = "mockValue";
    private static final String MOCK_ONE_KEY = "one";
    private static final String MOCK_TWO_KEY = "two";
    private static final String MOCK_THREE_KEY = "three";

    @BeforeEach
    public void init() {

        memConfig = new InmemoryConfiguration();
    }

    @Test
    public void testGetMemProperty() {
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertFalse(memConfig.containsKey(MOCK_KEY));
        Assertions.assertNull(memConfig.getString(MOCK_KEY));
        Assertions.assertNull(memConfig.getProperty(MOCK_KEY));
        memConfig.addProperty(MOCK_KEY, MOCK_VALUE);
        Assertions.assertTrue(memConfig.containsKey(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE, memConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE, memConfig.getString(MOCK_KEY, MOCK_VALUE));
        Assertions.assertEquals(MOCK_VALUE, memConfig.getProperty(MOCK_KEY, MOCK_VALUE));

    }

    @Test
    public void testGetProperties() {
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
        Map<String, String> proMap = new HashMap<>();
        proMap.put(MOCK_ONE_KEY, MOCK_VALUE);
        proMap.put(MOCK_TWO_KEY, MOCK_VALUE);
        memConfig.addProperties(proMap);
        Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
        Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_TWO_KEY));
        Map<String, String> anotherProMap = new HashMap<>();
        anotherProMap.put(MOCK_THREE_KEY, MOCK_VALUE);
        memConfig.setProperties(anotherProMap);
        Assertions.assertNotNull(memConfig.getInternalProperty(MOCK_THREE_KEY));
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_ONE_KEY));
        Assertions.assertNull(memConfig.getInternalProperty(MOCK_TWO_KEY));

    }
}