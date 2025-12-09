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
package org.apache.dubbo.xds.security.authz;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.security.api.AuthorizationException;
import org.apache.dubbo.xds.security.api.RequestAuthorizer;
import org.apache.dubbo.xds.security.authz.resolver.CredentialResolver;
import org.apache.dubbo.xds.security.authz.rule.CommonRequestCredential;
import org.apache.dubbo.xds.security.authz.rule.source.RuleFactory;
import org.apache.dubbo.xds.security.authz.rule.source.RuleProvider;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot.Action;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.config.validate.ValidationException;

@Activate
public class RoleBasedAuthorizer implements RequestAuthorizer {

    private final RuleProvider<?> ruleProvider;

    private final List<CredentialResolver> credentialResolver;

    private final RuleFactory ruleFactory;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RoleBasedAuthorizer.class);

    /**
     * TODO
     * Cached rules
     * Connection Identity -> Authorization Rules
     * Here are two problems:
     * 1.How to identify remote connection (may we can use [protocol:port])
     * 2.How to remove cache when remote connection is disconnected
     */
    private final Map<String, List<RuleRoot>> rules = new ConcurrentHashMap<>();

    public RoleBasedAuthorizer(ApplicationModel applicationModel) {
        this.ruleProvider = applicationModel.getAdaptiveExtension(RuleProvider.class);
        this.credentialResolver = applicationModel.getActivateExtensions(CredentialResolver.class);
        this.ruleFactory = applicationModel.getAdaptiveExtension(RuleFactory.class);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void validate(Invocation invocation) throws AuthorizationException {

        List rulesSources = ruleProvider.getSource(invocation.getInvoker().getUrl(), invocation);
        List<RuleRoot> roots = ruleFactory.getRules(invocation.getInvoker().getUrl(), rulesSources);

        List<RuleRoot> logRules = roots.stream()
                .filter(root -> root.getAction().equals(Action.LOG))
                .collect(Collectors.toList());

        roots.removeAll(logRules);

        List<RuleRoot> andRules = roots.stream()
                .filter(root -> Relation.AND.equals(root.getRelation()))
                .collect(Collectors.toList());
        List<RuleRoot> orRules = roots.stream()
                .filter(root -> Relation.OR.equals(root.getRelation()))
                .collect(Collectors.toList());
        List<RuleRoot> notRules = roots.stream()
                .filter(root -> Relation.NOT.equals(root.getRelation()))
                .collect(Collectors.toList());

        RequestCredential requestCredential = new CommonRequestCredential();
        credentialResolver.forEach(resolver ->
                resolver.appendRequestCredential(invocation.getInvoker().getUrl(), invocation, requestCredential));

        AuthorizationRequestContext context = new AuthorizationRequestContext(invocation, requestCredential);

        if (!logRules.isEmpty()) {
            context.startTrace();
            context.addTraceInfo(":::Start validation trace for request ["
                    + invocation.getInvoker().getUrl() + "], credentials=[" + invocation.getAttachments() + "] :::");

            for (RuleRoot logRule : logRules) {
                boolean result;
                try {
                    result = logRule.evaluate(context);
                    context.addTraceInfo("::: Request " + (result ? "meet" : "does not meet") + " rule ["
                            + logRule.getNodeName() + "] ::: ");
                } catch (ValidationException e) {
                    context.addTraceInfo(
                            "::: Got Exception evaluating rule [" + logRule.getNodeName() + "] , exception=" + e);
                }
            }
            context.addTraceInfo("::: End validation trace :::");
            context.endTrace();
            logger.info(context.getTraceInfo());
        }

        for (RuleRoot rule : notRules) {
            try {
                if (rule.evaluate(context) && rule.getAction().boolVal()) {
                    throw new AuthorizationException(
                            "Request authorization failed: request credential meet one of NOT rules.");
                }
            } catch (Exception e) {
                logger.error(
                        "",
                        "",
                        "",
                        "Request authorization failed, source:" + invocation.getServiceName() + // TODO get source
                                ", target URL:"
                                + invocation.getInvoker().getUrl(),
                        e.getCause());
                if (e instanceof AuthorizationException) {
                    throw (AuthorizationException) e;
                }
                throw new AuthorizationException(e);
            }
        }

        for (RuleRoot rule : andRules) {
            try {
                if (!rule.evaluate(context) && rule.getAction().boolVal()) {
                    throw new AuthorizationException(
                            "Request authorization failed: request credential doesn't meet all AND rules.");
                }
            } catch (Exception e) {
                logger.error(
                        "",
                        "",
                        "",
                        "Request authorization failed, source:" + invocation.getServiceName() + // TODO get source
                                ", target URL:"
                                + invocation.getInvoker().getUrl(),
                        e.getCause());
                if (e instanceof AuthorizationException) {
                    throw (AuthorizationException) e;
                }
                throw new AuthorizationException(e);
            }
        }

        boolean orRes = false;
        for (RuleRoot rule : orRules) {
            try {
                orRes = rule.evaluate(context) && rule.getAction().boolVal();
                if (orRes) {
                    break;
                }
            } catch (Exception e) {
                logger.error(
                        "",
                        "",
                        "",
                        "Request authorization failed, source:" + invocation.getServiceName() + // TODO source
                                ", target URL:"
                                + invocation.getInvoker().getUrl(),
                        e.getCause());
                if (e instanceof AuthorizationException) {
                    throw (AuthorizationException) e;
                }
                throw new AuthorizationException(e);
            }
        }
        if (CollectionUtils.isEmpty(orRules)) {
            orRes = true;
        }
        if (orRes) {
            return;
        }
        throw new AuthorizationException(
                "Request authorization failed: request credential doesn't meet any required " + "OR rules.");
    }
}
