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

package org.apache.dubbo.security.cert.rule.authorization;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.security.cert.Endpoint;

import java.net.UnknownHostException;
import java.util.List;

public class AuthorizationSource {
    private List<String> namespaces;
    private List<String> notNamespaces;
    private List<String> ipBlocks;
    private List<String> notIpBlocks;
    private List<String> principals;
    private List<String> notPrincipals;

    private List<AuthorizationExtend> sourceExtends;
    private List<AuthorizationExtend> sourceNotExtends;

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
    }

    public List<String> getNotNamespaces() {
        return notNamespaces;
    }

    public void setNotNamespaces(List<String> notNamespaces) {
        this.notNamespaces = notNamespaces;
    }

    public List<String> getIpBlocks() {
        return ipBlocks;
    }

    public void setIpBlocks(List<String> ipBlocks) {
        this.ipBlocks = ipBlocks;
    }

    public List<String> getNotIpBlocks() {
        return notIpBlocks;
    }

    public void setNotIpBlocks(List<String> notIpBlocks) {
        this.notIpBlocks = notIpBlocks;
    }

    public List<String> getPrincipals() {
        return principals;
    }

    public void setPrincipals(List<String> principals) {
        this.principals = principals;
    }

    public List<String> getNotPrincipals() {
        return notPrincipals;
    }

    public void setNotPrincipals(List<String> notPrincipals) {
        this.notPrincipals = notPrincipals;
    }

    public List<AuthorizationExtend> getSourceExtends() {
        return sourceExtends;
    }

    public void setSourceExtends(List<AuthorizationExtend> sourceExtends) {
        this.sourceExtends = sourceExtends;
    }

    public List<AuthorizationExtend> getSourceNotExtends() {
        return sourceNotExtends;
    }

    public void setSourceNotExtends(List<AuthorizationExtend> sourceNotExtends) {
        this.sourceNotExtends = sourceNotExtends;
    }

    public boolean match(Endpoint endpoint) {
        String namespace = endpoint.getNamespace();
        if (namespaces != null && !namespaces.isEmpty() &&
            (namespace == null || !namespaces.contains(namespace))) {
            return false;
        }

        if (notNamespaces != null && !notNamespaces.isEmpty() &&
            namespace != null && notNamespaces.contains(namespace)) {
            return false;
        }

        String ip = endpoint.getIp();
        if (ipBlocks != null && !ipBlocks.isEmpty() &&
            (ip == null || ipBlocks.stream().noneMatch(ipBlock -> {
                try {
                    return NetUtils.matchIpExpression(ipBlock, ip);
                } catch (UnknownHostException e) {
                    return false;
                }
            }))) {
            return false;
        }

        if (notIpBlocks != null && !notIpBlocks.isEmpty() &&
            ip != null && notIpBlocks.stream().anyMatch(ipBlock -> {
            try {
                return NetUtils.matchIpExpression(ipBlock, ip);
            } catch (UnknownHostException e) {
                return false;
            }
        })) {
            return false;
        }

        String principal = endpoint.getPrincipal();
        if (principals != null && !principals.isEmpty() &&
            (principal == null || !principals.contains(principal))) {
            return false;
        }

        if (notPrincipals != null && !notPrincipals.isEmpty() &&
            principal != null && notPrincipals.contains(principal)) {
            return false;
        }

        if (sourceExtends != null && !sourceExtends.isEmpty()) {
            for (AuthorizationExtend extend : sourceExtends) {
                if (!extend.match(endpoint)) {
                    return false;
                }
            }
        }

        if (sourceNotExtends != null && !sourceNotExtends.isEmpty()) {
            for (AuthorizationExtend extend : sourceNotExtends) {
                if (extend.match(endpoint)) {
                    return false;
                }
            }
        }

        return true;
    }
}
