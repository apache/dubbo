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
package org.apache.dubbo.rpc.cluster.router.expression;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

/**
 * Abstract router which observes to dynamic configuration
 * This is kind of copy from ListenableRouter.
 * Both of them shouldn't be listenable/observable, but listener/observer.
 * So the class name is named to ObserverRouter, not ObservableRouter.
 *
 * @author Weihua
 * @since 2.7.8
 */
public abstract class ObserverRouter extends AbstractRouter implements ConfigurationListener {
    public static final String NAME = "OBSERVER_ROUTER";
    private static final String RULE_SUFFIX = ".observer-router";

    public ObserverRouter(URL url, String ruleKey) {
        super(url);
        this.init(ruleKey);
    }

    private synchronized void init(String ruleKey) {
        if (StringUtils.isNotEmpty(ruleKey)) {
            String routerKey = ruleKey + RULE_SUFFIX;
            ruleRepository.addListener(routerKey, this);
            String rule = ruleRepository.getRule(routerKey, DynamicConfiguration.DEFAULT_GROUP);
            if (StringUtils.isNotEmpty(rule)) {
                this.process(new ConfigChangedEvent(routerKey, DynamicConfiguration.DEFAULT_GROUP, rule));
            }
        }
    }
}
