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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpBasedRequestCredential implements RequestCredential {

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

    private final Map<String, String> allCredentials;

    /**
     * credential properties -> prop path
     */
    private final Map<String, String> credentialPropPath;

    public HttpBasedRequestCredential(
            String issuer, String subject, String targetPath, String method, String namespace,Map<String, String> allCredentials) {
        this.issuer = issuer;
        this.subject = subject;
        this.targetPath = targetPath;
        this.method = method;
        this.namespace = namespace;
        this.allCredentials = allCredentials;
        this.credentialPropPath = new HashMap<>();
        credentialPropPath.put("rules.from.source.principals", this.issuer);
        credentialPropPath.put("rules.to.operation.methods", this.method);
        credentialPropPath.put("rules.to.operation.paths", this.targetPath);
        credentialPropPath.put("rules.from.source.namespaces",this.namespace);
    }

    @Override
    public List<List<String>> supportPaths() {
        return Arrays.asList(
                Arrays.asList("from", "source", "principals"),
                Arrays.asList("to", "operation", "methods"),
                Arrays.asList("to", "operation", "paths"));
    }

    @Override
    public List<String> getByPath(String mapPath) {
        String propertyPath = credentialPropPath.get(mapPath);
        if(StringUtils.isNotEmpty(propertyPath)) {
            return Collections.singletonList(propertyPath);
        }else {
            return Collections.emptyList();
        }
    }
}
