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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.config.AbstractConfig;

/**
 *
 */
public class ConfigConverter {
    private static final String[] SUFFIXES = new String[]{"Config", "Bean"};

    /**
     * @param config
     * @return
     */
    public static Configuration toConfiguration(AbstractConfig config) {
        InmemoryConfiguration configuration = new InmemoryConfiguration(config.getPrefix(), config.getId());
        configuration.addProperties(config.getMetaData());
        return configuration;
    }

}
