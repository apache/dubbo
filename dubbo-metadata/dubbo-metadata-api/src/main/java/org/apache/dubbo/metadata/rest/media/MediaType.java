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
package org.apache.dubbo.metadata.rest.media;


import java.util.Arrays;
import java.util.List;

public enum MediaType {
    ALL_VALUE("*/*"),
    APPLICATION_JSON_VALUE("application/json"),
    APPLICATION_FORM_URLENCODED_VALUE("application/x-www-form-urlencoded"),
    TEXT_PLAIN("text/plain"),
    TEXT_XML("text/xml"),
    OCTET_STREAM("application/octet-stream"),
    ;

    MediaType(String value) {
        this.value = value;
    }

    public String value;

    public static String getAllContentType() {

        MediaType[] values = MediaType.values();

        StringBuilder stringBuilder = new StringBuilder();

        for (MediaType mediaType : values) {
            stringBuilder.append(mediaType.value + " ");
        }
        return stringBuilder.toString();
    }

    public static List<MediaType> getSupportMediaTypes() {
        return Arrays.asList(APPLICATION_JSON_VALUE,
            APPLICATION_FORM_URLENCODED_VALUE,
            TEXT_PLAIN,TEXT_XML,OCTET_STREAM);
    }

}
