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
package org.apache.dubbo.integration;

import org.apache.dubbo.ZooKeeperServerTesting;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This abstraction class will implement some methods as base.
 * @param <T> the type of subclass of {@link IntegrationService}.
 */
public abstract class AbstractIntegrationTest<T> implements IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    /**
     * Start global config only once.
     */
    @BeforeAll
    public static void beforeAll() {
        //start zookeeper only once
        ZooKeeperServerTesting.start();
    }

    @BeforeEach
    public void before() {
        logger.info(getClass().getSimpleName()+ " testcase is beginning...");
        DubboBootstrap.reset();
        this.initialize();
    }

    @Test
    @Override
    public void integrate() {
        this.process();
    }

    /**
     * This abstract method should be implement by all subclass.
     *
     */
    public abstract void process();

    @AfterEach
    public void after() {
        this.destroy();
        DubboBootstrap.reset();
        logger.info(getClass().getSimpleName()+ " testcase is ending...");
    }

    /**
     * Destroy all global resources.
     */
    @AfterAll
    public static void afterAll() {
        // destroy zookeeper only once
        ZooKeeperServerTesting.shutdown();
    }
}
