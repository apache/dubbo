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
package org.apache.dubbo.rpc.cluster.router.script.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.script.ScriptStateRouter;
import org.apache.dubbo.rpc.cluster.router.script.config.model.ScriptRule;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_TAG_ROUTE_EMPTY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_TAG_ROUTE_INVALID;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_SCRIPT_TYPE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.FORCE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RUNTIME_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.TYPE_KEY;

public class AppScriptStateRouter<T> extends AbstractStateRouter<T> implements ConfigurationListener {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AppScriptStateRouter.class);
    private static final String RULE_SUFFIX = ".script-router";

    private ScriptRule scriptRule;
    private ScriptStateRouter<T> scriptRouter;
    private String application;

    public AppScriptStateRouter(URL url) {
        super(url);
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers,
                                          URL url,
                                          Invocation invocation,
                                          boolean needToPrintMessage,
                                          Holder<RouterSnapshotNode<T>> routerSnapshotNodeHolder,
                                          Holder<String> messageHolder) throws RpcException {
        if (scriptRouter == null || !scriptRule.isValid() || !scriptRule.isEnabled()) {
            if (needToPrintMessage) {
                messageHolder.set("Directly return from script router. Reason: Invokers from previous router is empty or script is not enabled. Script rule is: " + (scriptRule == null ? "null" : scriptRule.getRawRule()));
            }
            return invokers;
        }

        invokers = scriptRouter.route(invokers, url, invocation, needToPrintMessage, routerSnapshotNodeHolder);

        if (needToPrintMessage) {
            messageHolder.set(messageHolder.get());
        }

        return invokers;
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Notification of script rule change, type is: " + event.getChangeType() + ", raw rule is:\n " +
                event.getContent());
        }

        try {
            if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
                this.scriptRule = null;
            } else {
                this.scriptRule = ScriptRule.parse(event.getContent());
                URL scriptUrl = getUrl()
                    .addParameter(TYPE_KEY, isEmpty(scriptRule.getType()) ? DEFAULT_SCRIPT_TYPE_KEY : scriptRule.getType())
                    .addParameterAndEncoded(RULE_KEY, scriptRule.getScript())
                    .addParameter(FORCE_KEY, scriptRule.isForce())
                    .addParameter(RUNTIME_KEY, scriptRule.isRuntime());
                scriptRouter = new ScriptStateRouter<>(scriptUrl);
            }
        } catch (Exception e) {
            logger.error(CLUSTER_TAG_ROUTE_INVALID, "Failed to parse the raw tag router rule", "", "Failed to parse the raw tag router rule and it will not take effect, please check if the " +
                "rule matches with the template, the raw rule is:\n ", e);
        }
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return;
        }

        Invoker<T> invoker = invokers.get(0);
        URL url = invoker.getUrl();
        String providerApplication = url.getRemoteApplication();

        if (isEmpty(providerApplication)) {
            logger.error(CLUSTER_TAG_ROUTE_EMPTY, "tag router get providerApplication is empty", "", "TagRouter must getConfig from or subscribe to a specific application, but the application " +
                "in this TagRouter is not specified.");
            return;
        }

        synchronized (this) {
            if (!providerApplication.equals(application)) {
                if (StringUtils.isNotEmpty(application)) {
                    this.getRuleRepository().removeListener(application + RULE_SUFFIX, this);
                }
                String key = providerApplication + RULE_SUFFIX;
                this.getRuleRepository().addListener(key, this);
                application = providerApplication;
                String rawRule = this.getRuleRepository().getRule(key, DynamicConfiguration.DEFAULT_GROUP);
                if (StringUtils.isNotEmpty(rawRule)) {
                    this.process(new ConfigChangedEvent(key, DynamicConfiguration.DEFAULT_GROUP, rawRule));
                }
            }
        }
    }

    @Override
    public void stop() {
        if (StringUtils.isNotEmpty(application)) {
            this.getRuleRepository().removeListener(application + RULE_SUFFIX, this);
        }
    }

    // for testing purpose
    public void setScriptRule(ScriptRule scriptRule) {
        this.scriptRule = scriptRule;
    }
}
