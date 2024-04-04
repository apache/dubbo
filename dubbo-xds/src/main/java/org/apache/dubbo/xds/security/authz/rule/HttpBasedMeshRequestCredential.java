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
package org.apache.dubbo.xds.security.authz.rule;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.security.authz.RequestCredential;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auth0.jwt.interfaces.Claim;

public class HttpBasedMeshRequestCredential implements RequestCredential {

    private String principle;

    /**
     * who created and signed this request credential
     */
    private String issuer;

    /**
     * whom this credential refers to
     */
    private String subject;

    /**
     * Request PATH
     */
    private String targetPath;

    /**
     * The HTTP request methods like GET/POST
     */
    private String method;

    /**
     * namespace that request comes from
     */
    private String namespace;

    private String serviceName;

    private String serviceUid;

    private String podName;

    private String podId;

    private String version;

    private final Map<String, Claim> allCredentials;

    /**
     * credential properties -> prop path
     */
    private final Map<String, String> credentialPropPath;

    public HttpBasedMeshRequestCredential(Map<String, Claim> allCredentials) {
        this.allCredentials = allCredentials;
        this.credentialPropPath = new HashMap<>();

        // standard AuthorizationPolicy filtering items
        // destination.service.host and destination.workload.name, <serviceName>/<methodName>
        // non-standard properties, but can also use as filtering items
    }

    @Override
    public List<List<String>> supportPaths() {
        return null;
        //        return Arrays.asList(
        //                Arrays.asList("from", "source", "principals"),
        //                Arrays.asList("to", "operation", "methods"),
        //                Arrays.asList("to", "operation", "paths"));
    }

    @Override
    public List<String> getByPath(String mapPath) {
        String propertyPath = credentialPropPath.get(mapPath);
        if (StringUtils.isNotEmpty(propertyPath)) {
            return Collections.singletonList(propertyPath);
        } else {
            return Collections.emptyList();
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        credentialPropPath.put("rules.to.operation.paths", targetPath);
        this.targetPath = targetPath;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        credentialPropPath.put("rules.to.operation.methods", method);
        this.method = method;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        credentialPropPath.put("rules.from.source.namespaces", namespace);
        this.namespace = namespace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        credentialPropPath.put("rules.source.service.name", serviceName);
        this.serviceName = serviceName;
    }

    public String getServiceUid() {
        return serviceUid;
    }

    public void setServiceUid(String serviceUid) {
        credentialPropPath.put("rules.source.service.uid", serviceUid);
        this.serviceUid = serviceUid;
    }

    public Map<String, Claim> getAllCredentials() {
        return allCredentials;
    }

    public Map<String, String> getCredentialPropPath() {
        return credentialPropPath;
    }

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        credentialPropPath.put("rules.source.pod.name", podName);
        this.podName = podName;
    }

    public String getPodId() {
        return podId;
    }

    public void setPodId(String podId) {
        credentialPropPath.put("rules.source.pod.id", podId);
        this.podId = podId;
    }

    public String getPrinciple() {
        return principle;
    }

    public void setPrinciple(String principle) {
        credentialPropPath.put("rules.from.source.principals", principle);
        this.principle = principle;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        credentialPropPath.put("rules.to.operation.version", version);
        this.version = version;
    }
}
