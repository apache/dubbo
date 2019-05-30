package org.apache.dubbo.common.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
class EnvironmentConfigurationTest {

    private static EnvironmentConfiguration environmentConfig;
    private static final String MOCK_KEY = "mockKey";
    private static final String MOCK_VALUE = "mockValue";
    private static final String PATH_KEY="PATH";

    @BeforeEach
    public void init() {

        environmentConfig = new EnvironmentConfiguration();
    }

    @Test
    public void testGetInternalProperty(){
        Assertions.assertNull(environmentConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertEquals(System.getenv(PATH_KEY),environmentConfig.getInternalProperty(PATH_KEY));

    }

    @Test
    public void testContainsKey(){
        Assertions.assertTrue(environmentConfig.containsKey(PATH_KEY));
        Assertions.assertFalse(environmentConfig.containsKey(MOCK_KEY));
    }

    @Test
    public void testGetString(){
        Assertions.assertNull(environmentConfig.getString(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE,environmentConfig.getString(MOCK_KEY,MOCK_VALUE));
    }

    @Test
    public void testGetProperty(){
        Assertions.assertNull(environmentConfig.getProperty(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE,environmentConfig.getProperty(MOCK_KEY,MOCK_VALUE));
    }



}