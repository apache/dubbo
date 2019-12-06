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
package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.Objects;

/**
 * Application level router, "application.condition-router"
 */
public class AppRouter extends ListenableRouter {
    public static final String NAME = "APP_ROUTER";
    /**
     * AppRouter should after ServiceRouter
     */
    private static final int APP_ROUTER_DEFAULT_PRIORITY = 150;

    private static final Logger logger = LoggerFactory.getLogger(AppRouter.class);

    private String application;

    public AppRouter(URL url) {
        super(url);
        this.priority = APP_ROUTER_DEFAULT_PRIORITY;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }

        Invoker<T> invoker = invokers.get(0);
        URL url = invoker.getUrl();
        String remoteApplication = url.getParameter(CommonConstants.REMOTE_APPLICATION_KEY);

        if (StringUtils.isEmpty(remoteApplication)) {
            logger.error("AppRouter must getConfig from or subscribe to a specific application, but the application " +
                    "in this AppRouter is not specified.");
            return;
        }
        synchronized (this) {
            if (!Objects.equals(remoteApplication, application)) {
                String routerKey = remoteApplication + RULE_SUFFIX;
                ruleRepository.addListener(routerKey, this);
                application = remoteApplication;
                String rule = ruleRepository.getRule(routerKey, DynamicConfiguration.DEFAULT_GROUP);
                if (StringUtils.isNotEmpty(rule)) {
                    this.process(new ConfigChangedEvent(routerKey, DynamicConfiguration.DEFAULT_GROUP, rule));
                }
            }
        }
    }
}
