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

import java.util.Objects;

public final class MediaType {

    public static final String WILDCARD = "*";

    public static final String APPLICATION = "application";

    public static final String TEXT = "text";

    public static final String JSON = "json";

    public static final String XML = "xml";

    public static final String YAML = "yaml";

    public static final MediaType ALL = new MediaType(WILDCARD, WILDCARD);

    public static final MediaType APPLICATION_JSON = new MediaType(APPLICATION, JSON);

    public static final MediaType APPLICATION_XML = new MediaType(APPLICATION, XML);

    public static final MediaType APPLICATION_YAML = new MediaType(APPLICATION, YAML);

    public static final MediaType APPLICATION_JAVASCRIPT = new MediaType(APPLICATION, "javascript");

    public static final MediaType APPLICATION_OCTET_STREAM = new MediaType(APPLICATION, "octet-stream");

    public static final MediaType APPLICATION_GRPC = new MediaType(APPLICATION, "grpc");

    public static final MediaType APPLICATION_GRPC_PROTO = new MediaType(APPLICATION, "grpc+proto");

    public static final MediaType APPLICATION_FROM_URLENCODED = new MediaType(APPLICATION, "x-www-form-urlencoded");

    public static final MediaType MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");

    public static final MediaType TEXT_JSON = new MediaType(TEXT, JSON);

    public static final MediaType TEXT_XML = new MediaType(TEXT, XML);

    public static final MediaType TEXT_YAML = new MediaType(TEXT, YAML);

    public static final MediaType TEXT_CSS = new MediaType(TEXT, "css");

    public static final MediaType TEXT_JAVASCRIPT = new MediaType(TEXT, "javascript");

    public static final MediaType TEXT_HTML = new MediaType(TEXT, "html");

    public static final MediaType TEXT_PLAIN = new MediaType(TEXT, "plain");

    public static final MediaType TEXT_EVENT_STREAM = new MediaType(TEXT, "event-stream");

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

    public boolean isPureText() {
        return TEXT.equals(type);
    }

    public static MediaType of(String name) {
        Objects.requireNonNull(name);
        if (APPLICATION_JSON.name.equals(name)) {
            return APPLICATION_JSON;
        }
        if (APPLICATION_YAML.name.equals(name)) {
            return APPLICATION_YAML;
        }
        if (APPLICATION_FROM_URLENCODED.name.equals(name)) {
            return APPLICATION_FROM_URLENCODED;
        }
        int index = name.indexOf('/');
        if (index > 0) {
            return new MediaType(name.substring(0, index), name.substring(index + 1));
        }
        throw new IllegalArgumentException("Invalid media type: '" + name + "'");
    }
}
