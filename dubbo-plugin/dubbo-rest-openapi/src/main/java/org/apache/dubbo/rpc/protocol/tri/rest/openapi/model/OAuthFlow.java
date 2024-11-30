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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi.model;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OAuthFlow extends Node<OAuthFlow> {

    private String authorizationUrl;
    private String tokenUrl;
    private String refreshUrl;
    private Map<String, String> scopes;

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public OAuthFlow setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
        return this;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public OAuthFlow setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
        return this;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public OAuthFlow setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
        return this;
    }

    public Map<String, String> getScopes() {
        return scopes;
    }

    public OAuthFlow setScopes(Map<String, String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public OAuthFlow addScope(String name, String description) {
        if (scopes == null) {
            scopes = new LinkedHashMap<>();
        }
        scopes.put(name, description);
        return this;
    }

    public void removeScope(String name) {
        if (scopes != null) {
            scopes.remove(name);
        }
    }

    @Override
    public OAuthFlow clone() {
        OAuthFlow clone = super.clone();
        if (scopes != null) {
            clone.scopes = new LinkedHashMap<>(scopes);
        }
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "authorizationUrl", authorizationUrl);
        write(node, "tokenUrl", tokenUrl);
        write(node, "refreshUrl", refreshUrl);
        write(node, "scopes", scopes);
        writeExtensions(node);
        return node;
    }
}
