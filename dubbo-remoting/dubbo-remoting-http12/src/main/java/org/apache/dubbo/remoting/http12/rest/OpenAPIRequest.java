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
package org.apache.dubbo.remoting.http12.rest;

import org.apache.dubbo.common.utils.ToStringUtils;

import java.io.Serializable;

/**
 * OpenAPI request model.
 */
public class OpenAPIRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The openAPI group.
     */
    private String group;

    /**
     * The openAPI version, using a major.minor.patch versioning scheme
     * e.g. 1.0.1
     */
    private String version;

    /**
     * The openAPI tags. Each tag is an or condition.
     */
    private String[] tag;

    /**
     * The openAPI services. Each service is an or condition.
     */
    private String[] service;

    /**
     * The openAPI specification version, using a major.minor.patch versioning scheme
     * e.g. 3.0.1, 3.1.0
     * <p>The default value is '3.0.1'.
     */
    @Schema(enumeration = {"3.0.1", "3.1.0"})
    private String openapi;

    /**
     * The format of the response.
     * <p>The default value is 'json'.
     */
    @Schema(enumeration = {"json", "yaml", "proto"})
    private String format;

    /**
     * Whether to pretty print for json.
     * <p>The default value is {@code false}.
     */
    private Boolean pretty;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String[] getTag() {
        return tag;
    }

    public void setTag(String[] tag) {
        this.tag = tag;
    }

    public String[] getService() {
        return service;
    }

    public void setService(String[] service) {
        this.service = service;
    }

    public String getOpenapi() {
        return openapi;
    }

    public void setOpenapi(String openapi) {
        this.openapi = openapi;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Boolean getPretty() {
        return pretty;
    }

    public void setPretty(Boolean pretty) {
        this.pretty = pretty;
    }

    @Override
    public String toString() {
        return ToStringUtils.printToString(this);
    }
}
