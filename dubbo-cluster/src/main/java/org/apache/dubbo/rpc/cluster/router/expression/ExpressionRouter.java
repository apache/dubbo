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
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.router.expression.context.ContextBuilder;
import org.apache.dubbo.rpc.cluster.router.expression.model.Rule;
import org.apache.dubbo.rpc.cluster.router.expression.model.RuleSet;
import org.apache.dubbo.rpc.cluster.router.expression.model.ExpressionRuleConstructor;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.yaml.snakeyaml.Yaml;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ExpressionRouter which observes the change of Config Center for the key of <Application_Name>.observer-router.
 * RuleSets dynamically changes according to the related value.
 *
 * @author Weihua
 * @since 2.7.8
 */
public class ExpressionRouter extends ObserverRouter {

    public static final String NAME = "expression";

    private static final Logger logger = LoggerFactory.getLogger(ExpressionRouter.class);

    /**
     * Store the mapping relations of provider/ruleSet.
     */
    private static final Map<String, RuleSet> ruleSets = new ConcurrentHashMap<>();

    private static final JexlEngine engine = new JexlBuilder().create();

    private ContextBuilder contextBuilder;

    public ExpressionRouter(URL url) {
        super(url, url.getParameter(CommonConstants.APPLICATION_KEY));
        contextBuilder = ExtensionLoader.getExtensionLoader(ContextBuilder.class)
                .getExtension(url.getParameter(Constants.CONTEXT_BUILDER_KEY, Constants.DEFAULT_CONTEXT_BUILDER));
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        String application = url.getParameter(CommonConstants.REMOTE_APPLICATION_KEY);
        RuleSet ruleSet = ruleSets.get(application);
        if (logger.isTraceEnabled()) {
            logger.trace(ruleSet.toString());
        }
        if (ruleSet != null && ruleSet.isEnabled()) {
            JexlContext clientContext = new MapContext();
            contextBuilder.buildClientContext(url, invocation).forEach(clientContext::set);
            for (Rule rule : ruleSet.getRules()) {
                Object clientQualified = engine.createExpression(rule.getClientCondition()).evaluate(clientContext);
                if (clientQualified instanceof Boolean && (Boolean) clientQualified) {
                    List<Invoker<T>> result = invokers
                            .stream()
                            .filter(invoker -> matches(contextBuilder.buildServerContext(invoker, url, invocation), rule.getServerQuery()))
                            .collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(result)) {
                        return result;
                    }
                }
            }
            if (ruleSet.isDefaultRuleEnabled()) {
                return invokers;
            } else {
                return Collections.emptyList();
            }
        }
        return invokers;
    }

    public boolean matches(Map<String, Object> objects, String expression) {
        JexlContext context = new MapContext();
        objects.forEach(context::set);
        Object qualified = engine.createExpression(expression).evaluate(context);
        return qualified instanceof Boolean && (Boolean) qualified;
    }

    @Override
    public void process(ConfigChangedEvent event) {
        if (event.getChangeType().equals(ConfigChangeType.DELETED)) {
            ruleSets.clear();
        } else {
            try {
                ruleSets.clear();
                ruleSets.putAll(new Yaml(new ExpressionRuleConstructor()).load(event.getContent()));
                logger.info(String.format("Expression router rules was loaded for %s provider(s).", ruleSets.size()));
            } catch (Exception e) {
                logger.error("Failed to parse the raw condition rule and it will not take effect, please check " +
                        "if the expression rule matches with the template, the raw rule is:\n " + event.getContent(), e);
            }
        }
    }
}
