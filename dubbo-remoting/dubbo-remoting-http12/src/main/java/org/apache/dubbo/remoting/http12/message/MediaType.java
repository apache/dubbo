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
package org.apache.dubbo.remoting.http12.message;

public final class MediaType {

    public static final String WILDCARD = "*";

    public static final MediaType ALL = new MediaType(WILDCARD, WILDCARD);

    public static final MediaType APPLICATION_JSON = new MediaType("application", "json");

    public static final MediaType APPLICATION_XML = new MediaType("application", "xml");

    public static final MediaType APPLICATION_YAML = new MediaType("application", "yaml");

    public static final MediaType APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");

    public static final MediaType APPLICATION_GRPC = new MediaType("application", "grpc");

    public static final MediaType APPLICATION_GRPC_PROTO = new MediaType("application", "grpc+proto");

    public static final MediaType APPLICATION_FROM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");

    public static final MediaType MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");

    public static final MediaType TEXT_JSON = new MediaType("text", "json");

    public static final MediaType TEXT_XML = new MediaType("text", "xml");

    public static final MediaType TEXT_YAML = new MediaType("text", "yaml");

    public static final MediaType TEXT_HTML = new MediaType("text", "html");

    public static final MediaType TEXT_PLAIN = new MediaType("text", "plain");

    public static final MediaType TEXT_EVENT_STREAM = new MediaType("text", "event-stream");

    private final String name;

    private final String type;

    private final String subType;

    public MediaType(String type, String subType) {
        this.type = type;
        this.subType = subType;
        this.name = type + '/' + subType;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }
}
