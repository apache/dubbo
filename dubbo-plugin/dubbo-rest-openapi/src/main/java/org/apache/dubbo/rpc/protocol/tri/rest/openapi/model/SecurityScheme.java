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

public final class SecurityScheme extends Node<SecurityScheme> {

    public enum Type {
        APIKEY("apiKey"),
        HTTP("http"),
        OAUTH2("oauth2"),
        MUTUAL_TLS("mutualTLS"),
        OPEN_ID_CONNECT("openIdConnect");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum In {
        COOKIE("cookie"),
        HEADER("header"),
        QUERY("query");

        private final String value;

        In(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private Type type;
    private String description;
    private String name;
    private In in;
    private String scheme;
    private String bearerFormat;
    private OAuthFlows flows;
    private String openIdConnectUrl;

    public Type getType() {
        return type;
    }

    public SecurityScheme setType(Type type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SecurityScheme setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public SecurityScheme setName(String name) {
        this.name = name;
        return this;
    }

    public In getIn() {
        return in;
    }

    public SecurityScheme setIn(In in) {
        this.in = in;
        return this;
    }

    public String getScheme() {
        return scheme;
    }

    public SecurityScheme setScheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public SecurityScheme setBearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
        return this;
    }

    public OAuthFlows getFlows() {
        return flows;
    }

    public SecurityScheme setFlows(OAuthFlows flows) {
        this.flows = flows;
        return this;
    }

    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public SecurityScheme setOpenIdConnectUrl(String openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
        return this;
    }

    @Override
    public SecurityScheme clone() {
        SecurityScheme clone = super.clone();
        clone.flows = clone(flows);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        if (type == null) {
            return node;
        }
        write(node, "type", type.toString());
        write(node, "description", description);
        write(node, "name", name);
        if (in != null) {
            write(node, "in", in.toString());
        }
        write(node, "scheme", scheme);
        write(node, "bearerFormat", bearerFormat);
        write(node, "flows", flows, context);
        write(node, "openIdConnectUrl", openIdConnectUrl);
        writeExtensions(node);
        return node;
    }
}
