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

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public class MediaType {

    public static final MediaType ALL_VALUE = new MediaType("*", "*");

    public static final MediaType APPLICATION_JSON_VALUE = new MediaType("application", "json");

    public static final MediaType TEXT_EVENT_STREAM_VALUE = new MediaType("text", "event-stream");

    public static final MediaType MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");

    public static final MediaType APPLICATION_X_WWW_FROM_URLENCODED =
            new MediaType("application", "x-www-form-urlencoded");

    public static final MediaType APPLICATION_XML = new MediaType("application", "xml");

    public static final MediaType TEXT_PLAIN = new MediaType("text", "plain");

    private final String name;

    private final String type;

    private final String subType;

    private final Charset charset;

    public MediaType(String type, String subType) {
        this(type, subType, Collections.singletonMap("charset", "UTF-8"));
    }

    public MediaType(String type, String subType, Map<String, String> parameters) {
        this.type = type;
        this.subType = subType;
        this.name = type + "/" + subType;
        this.charset = Charset.forName(parameters.getOrDefault("charset", "UTF-8"));
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

    public Charset getCharset() {
        return charset;
    }
}
