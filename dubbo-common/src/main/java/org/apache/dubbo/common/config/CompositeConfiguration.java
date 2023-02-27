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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_LOAD_ENV_VARIABLE;

/**
 * This is an abstraction specially customized for the sequence Dubbo retrieves properties.
 */
public class CompositeConfiguration implements Configuration {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CompositeConfiguration.class);

    /**
     * List holding all the configuration
     */
    private final List<Configuration> configList = new CopyOnWriteArrayList<>();

    //FIXME, consider change configList to SortedMap to replace this boolean status.
    private boolean dynamicIncluded;
<<<<<<< HEAD

    public CompositeConfiguration() {
        this(null, null);
    }
=======
>>>>>>> origin/3.2

    public CompositeConfiguration() {
    }

    public CompositeConfiguration(Configuration... configurations) {
        if (ArrayUtils.isNotEmpty(configurations)) {
            Arrays.stream(configurations).filter(config -> !configList.contains(config)).forEach(configList::add);
        }
    }

<<<<<<< HEAD
    public void setDynamicIncluded(boolean dynamicIncluded) {
        this.dynamicIncluded = dynamicIncluded;
    }

    //FIXME, consider change configList to SortedMap to replace this boolean status.
=======
    //FIXME, consider changing configList to SortedMap to replace this boolean status.
>>>>>>> origin/3.2
    public boolean isDynamicIncluded() {
        return dynamicIncluded;
    }

<<<<<<< HEAD
=======
    public void setDynamicIncluded(boolean dynamicIncluded) {
        this.dynamicIncluded = dynamicIncluded;
    }

>>>>>>> origin/3.2
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
    public Object getInternalProperty(String key) {
        for (Configuration config : configList) {
            try {
                Object value = config.getProperty(key);
                if (!ConfigurationUtils.isEmptyValue(value)) {
                    return value;
                }
            } catch (Exception e) {
                logger.error(CONFIG_FAILED_LOAD_ENV_VARIABLE, "", "", "Error when trying to get value for key " + key + " from " + config + ", " +
                    "will continue to try the next one.");
            }
        }
        return null;
    }

<<<<<<< HEAD
    @Override
    public boolean containsKey(String key) {
        return configList.stream().anyMatch(c -> c.containsKey(key));
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        Object value = null;
        if (StringUtils.isNotEmpty(prefix)) {
            if (StringUtils.isNotEmpty(id)) {
                value = getInternalProperty(prefix + id + "." + key);
            }
            if (value == null) {
                value = getInternalProperty(prefix + key);
            }
        } else {
            value = getInternalProperty(key);
        }
        return value != null ? value : defaultValue;
    }
}
=======
}
>>>>>>> origin/3.2
