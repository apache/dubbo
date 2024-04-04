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
import org.apache.dubbo.xds.security.authz.rule.CredentialFactory;
import org.apache.dubbo.xds.security.authz.rule.RuleFactory;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Activate
public class RoleBasedAuthorizer implements RequestAuthorizer {

    private final RuleSourceProvider ruleSourceProvider;

    private final CredentialFactory credentialFactory;

    private final RuleFactory ruleFactory;

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RoleBasedAuthorizer.class);

    /**
     * TODO
     * Cached rules
     * Connection Identity -> Authorization Rules
     */
    private final Map<String, List<RuleNode>> rules = new ConcurrentHashMap<>();

    public RoleBasedAuthorizer(ApplicationModel applicationModel) {
        this.ruleSourceProvider = applicationModel.getAdaptiveExtension(RuleSourceProvider.class);
        this.credentialFactory = applicationModel.getAdaptiveExtension(CredentialFactory.class);
        this.ruleFactory = applicationModel.getBeanFactory().getBean(RuleFactory.class);
    }

    @Override
    public void validate(Invocation invocation) throws AuthorizationException {

        List<RuleSource> rulesSources =
                ruleSourceProvider.getSource(invocation.getInvoker().getUrl(), invocation);

        List<RuleRoot> andRules = new ArrayList<>();
        List<RuleRoot> orRules = new ArrayList<>();

        for (RuleSource source : rulesSources) {
            List<RuleRoot> roots = ruleFactory.getRules(source);
            andRules.addAll(roots.stream()
                    .filter(root -> Relation.AND.equals(root.getRelation()))
                    .collect(Collectors.toList()));
            orRules.addAll(roots.stream()
                    .filter(root -> Relation.OR.equals(root.getRelation()))
                    .collect(Collectors.toList()));
        }

        RequestCredential requestCredential =
                credentialFactory.getRequestCredential(invocation.getInvoker().getUrl(), invocation);

        AuthorizationRequestContext context = new AuthorizationRequestContext(invocation, requestCredential);

        boolean andRes = true;
        for (RuleRoot rule : andRules) {
            try {
                if (!rule.evaluate(context) && rule.getAction().boolVal()) {
                    andRes = false;
                    break;
                }
            } catch (Exception e) {
                logger.error(
                        "",
                        "",
                        "",
                        "Request authorization failed, source:" + invocation.getServiceName() + // TODO get source
                                ", target URL:" + invocation.getInvoker().getUrl(),
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
                                ", target URL:" + invocation.getInvoker().getUrl(),
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
        if (andRes && orRes) {
            return;
        }
        throw new AuthorizationException("Request authorization failed: request credential doesn't meet rules.");
    }
}
