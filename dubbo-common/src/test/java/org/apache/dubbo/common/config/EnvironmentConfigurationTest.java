package org.apache.dubbo.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The type Environment configuration test.
 */
class EnvironmentConfigurationTest {

    private static EnvironmentConfiguration environmentConfig;
    private static final String MOCK_KEY = "mockKey";
    private static final String MOCK_VALUE = "mockValue";
    private static final String PATH_KEY="PATH";

    /**
     * Init.
     */
    @BeforeEach
    public void init() {

        environmentConfig = new EnvironmentConfiguration();
    }

    /**
     * Test get internal property.
     */
    @Test
    public void testGetInternalProperty(){
        Assertions.assertNull(environmentConfig.getInternalProperty(MOCK_KEY));
        Assertions.assertEquals(System.getenv(PATH_KEY),environmentConfig.getInternalProperty(PATH_KEY));

    }

    /**
     * Test contains key.
     */
    @Test
    public void testContainsKey(){
        Assertions.assertTrue(environmentConfig.containsKey(PATH_KEY));
        Assertions.assertFalse(environmentConfig.containsKey(MOCK_KEY));
    }

    /**
     * Test get string.
     */
    @Test
    public void testGetString(){
        Assertions.assertNull(environmentConfig.getString(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE,environmentConfig.getString(MOCK_KEY,MOCK_VALUE));
    }

    /**
     * Test get property.
     */
    @Test
    public void testGetProperty(){
        Assertions.assertNull(environmentConfig.getProperty(MOCK_KEY));
        Assertions.assertEquals(MOCK_VALUE,environmentConfig.getProperty(MOCK_KEY,MOCK_VALUE));
    }

    /**
     * Clean.
     */
    @AfterEach
    public void clean(){

    }

}