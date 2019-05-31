/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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