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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class CompositeConfiguration extends AbstractConfiguration {

    /**
     * List holding all the configuration
     */
    private List<Configuration> configList = new LinkedList<Configuration>();

    public CompositeConfiguration() {

    }

    public CompositeConfiguration(Configuration... configurations) {
        if (configurations != null && configurations.length > 0) {
            Arrays.stream(configurations).filter(config -> !configList.contains(config)).forEach(configList::add);
        }
    }

    public void addConfiguration(Configuration configuration) {
        if (configList.contains(configuration)) {
            return;
        }
        this.configList.add(configuration);
    }

    public void addConfigurationFirst(Configuration configuration) {
        this.addConfiguration(0, configuration);
    }

    public void addConfiguration(int pos, Configuration configuration) {
        this.configList.add(pos, configuration);
    }

    @Override
    protected Object getInternalProperty(String key) {
        Configuration firstMatchingConfiguration = null;
        for (Configuration config : configList) {
            try {
                if (config.containsKey(key)) {
                    firstMatchingConfiguration = config;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (firstMatchingConfiguration != null) {
            return firstMatchingConfiguration.getProperty(key);
        } else {
            return null;
        }
    }

    @Override
    public boolean containsKey(String key) {
        return configList.stream().anyMatch(c -> c.containsKey(key));
    }
}
