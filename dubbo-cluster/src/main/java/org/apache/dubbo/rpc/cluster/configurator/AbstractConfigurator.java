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
package org.apache.dubbo.rpc.cluster.configurator;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConditionMatch;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACES;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.COMPATIBLE_CONFIG_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.CONFIG_VERSION_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.OVERRIDE_PROVIDERS_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_VERSION_V30;
import static org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig.MATCH_CONDITION;

/**
 * AbstractConfigurator
 */
public abstract class AbstractConfigurator implements Configurator {
    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigurator.class);
    private static final String TILDE = "~";

    private final URL configuratorUrl;

    public AbstractConfigurator(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("configurator url == null");
        }
        this.configuratorUrl = url;
    }

    @Override
    public URL getUrl() {
        return configuratorUrl;
    }

    @Override
    public URL configure(URL url) {
        // If override url is not enabled or is invalid, just return.
        if (!configuratorUrl.getParameter(ENABLED_KEY, true) || configuratorUrl.getHost() == null || url == null || url.getHost() == null) {
            logger.info("Cannot apply configurator rule, the rule is disabled or is invalid: \n" + configuratorUrl);
            return url;
        }

        String apiVersion = configuratorUrl.getParameter(CONFIG_VERSION_KEY);
        if (StringUtils.isNotEmpty(apiVersion)) { // v2.7 or above
            String currentSide = url.getSide();
            String configuratorSide = configuratorUrl.getSide();
            if (currentSide.equals(configuratorSide) && CONSUMER.equals(configuratorSide)) {
                url = configureIfMatch(NetUtils.getLocalHost(), url);
            } else if (currentSide.equals(configuratorSide) && PROVIDER.equals(configuratorSide)) {
                url = configureIfMatch(url.getHost(), url);
            }
        }
        /*
         * This else branch is deprecated and is left only to keep compatibility with versions before 2.7.0
         */
        else {
            url = configureDeprecated(url);
        }
        return url;
    }

    @Deprecated
    private URL configureDeprecated(URL url) {
        // If override url has port, means it is a provider address. We want to control a specific provider with this override url, it may take effect on the specific provider instance or on consumers holding this provider instance.
        if (configuratorUrl.getPort() != 0) {
            if (url.getPort() == configuratorUrl.getPort()) {
                return configureIfMatch(url.getHost(), url);
            }
        } else {
            /*
             *  override url don't have a port, means the ip override url specify is a consumer address or 0.0.0.0.
             *  1.If it is a consumer ip address, the intention is to control a specific consumer instance, it must takes effect at the consumer side, any provider received this override url should ignore.
             *  2.If the ip is 0.0.0.0, this override url can be used on consumer, and also can be used on provider.
             */
            if (url.getSide(PROVIDER).equals(CONSUMER)) {
                // NetUtils.getLocalHost is the ip address consumer registered to registry.
                return configureIfMatch(NetUtils.getLocalHost(), url);
            } else if (url.getSide(CONSUMER).equals(PROVIDER)) {
                // take effect on all providers, so address must be 0.0.0.0, otherwise it won't flow to this if branch
                return configureIfMatch(ANYHOST_VALUE, url);
            }
        }
        return url;
    }

    private URL configureIfMatch(String host, URL url) {
        if (ANYHOST_VALUE.equals(configuratorUrl.getHost()) || host.equals(configuratorUrl.getHost())) {
            if (isV27ConditionMatchOrUnset(url)) {
                Set<String> conditionKeys = genConditionKeys();
                String apiVersion = configuratorUrl.getParameter(CONFIG_VERSION_KEY);
                if (apiVersion != null && apiVersion.startsWith(RULE_VERSION_V30)) {
                    ConditionMatch matcher = (ConditionMatch) configuratorUrl.getAttribute(MATCH_CONDITION);
                    if (matcher != null) {
                        if (matcher.isMatch(url)) {
                            return doConfigure(url, configuratorUrl.removeParameters(conditionKeys));
                        } else {
                            logger.debug("Cannot apply configurator rule, param mismatch, current params are " + url + ", params in rule is " + matcher);
                        }
                    } else {
                        return doConfigure(url, configuratorUrl.removeParameters(conditionKeys));
                    }
                } else if (isDeprecatedConditionMatch(conditionKeys, url)) {
                    return doConfigure(url, configuratorUrl.removeParameters(conditionKeys));
                }
            }
        } else {
            logger.debug("Cannot apply configurator rule, host mismatch, current host is " + host + ", host in rule is " + configuratorUrl.getHost());
        }
        return url;
    }

    /**
     * Check if v2.7 configurator rule is set and can be matched.
     *
     * @param url the configurator rule url
     * @return true if v2.7 configurator rule is not set or the rule can be matched.
     */
    private boolean isV27ConditionMatchOrUnset(URL url) {
        String providers = configuratorUrl.getParameter(OVERRIDE_PROVIDERS_KEY);
        if (StringUtils.isNotEmpty(providers)) {
            boolean match = false;
            String[] providerAddresses = providers.split(CommonConstants.COMMA_SEPARATOR);
            for (String address : providerAddresses) {
                if (address.equals(url.getAddress())
                    || address.equals(ANYHOST_VALUE)
                    || address.equals(ANYHOST_VALUE + CommonConstants.GROUP_CHAR_SEPARATOR + ANY_VALUE)
                    || address.equals(ANYHOST_VALUE + CommonConstants.GROUP_CHAR_SEPARATOR + url.getPort())
                    || address.equals(url.getHost())) {
                    match = true;
                }
            }
            if (!match) {
                logger.debug("Cannot apply configurator rule, provider address mismatch, current address " + url.getAddress() + ", address in rule is " + providers);
                return false;
            }
        }

        String configApplication = configuratorUrl.getApplication(configuratorUrl.getUsername());
        String currentApplication = url.getApplication(url.getUsername());
        if (configApplication != null
            && !ANY_VALUE.equals(configApplication)
            && !configApplication.equals(currentApplication)) {
            logger.debug("Cannot apply configurator rule, application name mismatch, current application is " + currentApplication + ", application in rule is " + configApplication);
            return false;
        }

        String configServiceKey = configuratorUrl.getServiceKey();
        String currentServiceKey = url.getServiceKey();
        if (!ANY_VALUE.equals(configServiceKey)
            && !configServiceKey.equals(currentServiceKey)) {
            logger.debug("Cannot apply configurator rule, service mismatch, current service is " + currentServiceKey + ", service in rule is " + configServiceKey);
            return false;
        }

        return true;
    }

    private boolean isDeprecatedConditionMatch(Set<String> conditionKeys, URL url) {
        boolean result = true;
        for (Map.Entry<String, String> entry : configuratorUrl.getParameters().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            boolean startWithTilde = startWithTilde(key);
            if (startWithTilde || APPLICATION_KEY.equals(key) || SIDE_KEY.equals(key)) {
                if (startWithTilde) {
                    conditionKeys.add(key);
                }
                if (value != null && !ANY_VALUE.equals(value)
                    && !value.equals(url.getParameter(startWithTilde ? key.substring(1) : key))) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    private Set<String> genConditionKeys() {
        Set<String> conditionKeys = new HashSet<String>();
        conditionKeys.add(CATEGORY_KEY);
        conditionKeys.add(Constants.CHECK_KEY);
        conditionKeys.add(DYNAMIC_KEY);
        conditionKeys.add(ENABLED_KEY);
        conditionKeys.add(GROUP_KEY);
        conditionKeys.add(VERSION_KEY);
        conditionKeys.add(APPLICATION_KEY);
        conditionKeys.add(SIDE_KEY);
        conditionKeys.add(CONFIG_VERSION_KEY);
        conditionKeys.add(COMPATIBLE_CONFIG_KEY);
        conditionKeys.add(INTERFACES);
        return conditionKeys;
    }

    private boolean startWithTilde(String key) {
        return StringUtils.isNotEmpty(key) && key.startsWith(TILDE);
    }

    protected abstract URL doConfigure(URL currentUrl, URL configUrl);

}
