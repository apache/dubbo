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

public final class Encoding extends Node<Encoding> {

    public enum Style {
        FORM("form"),
        SPACE_DELIMITED("spaceDelimited"),
        PIPE_DELIMITED("pipeDelimited"),
        DEEP_OBJECT("deepObject");

        private final String value;

        Style(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private String contentType;
    private Map<String, Parameter> headers;
    private Style style;
    private Boolean explode;
    private Boolean allowReserved;

    public String getContentType() {
        return contentType;
    }

    public Encoding setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Map<String, Parameter> getHeaders() {
        return headers;
    }

    public Parameter getHeader(String name) {
        return headers == null ? null : headers.get(name);
    }

    public Encoding setHeaders(Map<String, Parameter> headers) {
        this.headers = headers;
        return this;
    }

    public Encoding addHeader(String name, Parameter header) {
        if (headers == null) {
            headers = new LinkedHashMap<>();
        }
        headers.put(name, header);
        return this;
    }

    public Encoding removeHeader(String name) {
        if (headers != null) {
            headers.remove(name);
        }
        return this;
    }

    public Style getStyle() {
        return style;
    }

    public Encoding setStyle(Style style) {
        this.style = style;
        return this;
    }

    public Boolean getExplode() {
        return explode;
    }

    public Encoding setExplode(Boolean explode) {
        this.explode = explode;
        return this;
    }

    public Boolean getAllowReserved() {
        return allowReserved;
    }

    public Encoding setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
        return this;
    }

    @Override
    public Encoding clone() {
        Encoding clone = super.clone();
        clone.headers = clone(headers);
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> encoding, Context context) {
        write(encoding, "contentType", contentType);
        write(encoding, "headers", headers, context);
        write(encoding, "style", style);
        write(encoding, "explode", explode);
        write(encoding, "allowReserved", allowReserved);
        writeExtensions(encoding);
        return encoding;
    }
}
