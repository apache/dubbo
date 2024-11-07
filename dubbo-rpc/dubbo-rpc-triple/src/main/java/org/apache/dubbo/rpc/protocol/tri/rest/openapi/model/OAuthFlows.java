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

import java.util.Map;

public final class OAuthFlows extends Node<OAuthFlows> {

    private OAuthFlow implicit;
    private OAuthFlow password;
    private OAuthFlow clientCredentials;
    private OAuthFlow authorizationCode;

    public OAuthFlow getImplicit() {
        return implicit;
    }

    public OAuthFlows setImplicit(OAuthFlow implicit) {
        this.implicit = implicit;
        return this;
    }

    public OAuthFlow getPassword() {
        return password;
    }

    public OAuthFlows setPassword(OAuthFlow password) {
        this.password = password;
        return this;
    }

    public OAuthFlow getClientCredentials() {
        return clientCredentials;
    }

    public OAuthFlows setClientCredentials(OAuthFlow clientCredentials) {
        this.clientCredentials = clientCredentials;
        return this;
    }

    public OAuthFlow getAuthorizationCode() {
        return authorizationCode;
    }

    public OAuthFlows setAuthorizationCode(OAuthFlow authorizationCode) {
        this.authorizationCode = authorizationCode;
        return this;
    }

    @Override
    public OAuthFlows clone() {
        OAuthFlows clone = super.clone();
        clone.implicit = clone(implicit);
        clone.password = clone(password);
        clone.clientCredentials = clone(clientCredentials);
        clone.authorizationCode = clone(authorizationCode);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "implicit", implicit);
        write(node, "password", password);
        write(node, "clientCredentials", clientCredentials);
        write(node, "authorizationCode", authorizationCode);
        writeExtensions(node);
        return node;
    }
}
