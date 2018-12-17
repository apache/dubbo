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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigChangeType;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.rpc.cluster.configurator.parser.ConfigParser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract class AbstractConfiguratorListener implements ConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(AbstractConfiguratorListener.class);

    @Override
    public void process(ConfigChangeEvent event) {
        if (logger.isInfoEnabled()) {
            logger.info("Notification of overriding rule, change type is: " + event.getChangeType() + ", raw config content is:\n " + event
                    .getValue());
        }

        List<URL> urls;
        if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
            urls = new ArrayList<>();
            urls.add(new URL(Constants.EMPTY_PROTOCOL, Constants.ANYHOST_VALUE, 0));
        } else {
            try {
                // parseConfigurators will recognize app/service config automatically.
                urls = ConfigParser.parseConfigurators(event.getValue());
            } catch (Exception e) {
                logger.error("Failed to parse raw dynamic config and it will not take effect, the raw config is: " + event
                        .getValue(), e);
                return;
            }
        }

        logger.debug("Successfully transformed override rule to urls, will do override now, the urls are: " + urls);
        notifyOverrides(urls);
    }

    protected abstract void notifyOverrides(List<URL> urls);
}
