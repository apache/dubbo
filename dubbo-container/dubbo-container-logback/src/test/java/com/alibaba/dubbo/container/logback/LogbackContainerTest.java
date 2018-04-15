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
package com.alibaba.dubbo.container.logback;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.container.Container;

import org.junit.Test;

/**
 * StandaloneContainerTest
 */
public class LogbackContainerTest {

    private static final Logger logger = LoggerFactory.getLogger(LogbackContainerTest.class);

    @Test
    public void testContainer() {
        LogbackContainer container = (LogbackContainer) ExtensionLoader.getExtensionLoader(Container.class)
                .getExtension("logback");
        container.start();

        logger.debug("Test debug:" + this.getClass().getName());
        logger.warn("Test warn:" + this.getClass().getName());
        logger.info("Test info:" + this.getClass().getName());
        logger.error("Test error:" + this.getClass().getName());

        container.stop();
    }

}