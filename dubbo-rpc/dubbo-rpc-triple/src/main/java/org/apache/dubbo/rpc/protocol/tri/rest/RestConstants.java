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
package org.apache.dubbo.rpc.protocol.tri.rest;

import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

public final class RestConstants {

    public static final String REST = "rest";

    public static final String REST_FILTER_KEY = "rest.filter";
    public static final String EXTENSION_KEY = "extension";
    public static final String EXTENSIONS_ATTRIBUTE_KEY = "restExtensionsAttributeKey";

    public static final int DIALECT_SPRING_MVC = 1;
    public static final int DIALECT_JAXRS = 2;

    public static final String HEADER_SERVICE_VERSION = "rest-service-version";
    public static final String HEADER_SERVICE_GROUP = "rest-service-group";

    public static final String SLASH = "/";

    /* Request Attribute */
    public static final String BODY_ATTRIBUTE = HttpRequest.class.getName() + ".body";
    public static final String BODY_DECODER_ATTRIBUTE = HttpMessageDecoder.class.getName() + ".body";
    public static final String MAPPING_ATTRIBUTE = RequestMapping.class.getName();
    public static final String HANDLER_ATTRIBUTE = HandlerMeta.class.getName();
    public static final String PATH_ATTRIBUTE = "org.springframework.web.util.UrlPathHelper.PATH";
    public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE =
            "org.springframework.web.servlet.HandlerMapping.uriTemplateVariables";
    public static final String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE =
            "org.springframework.web.servlet.HandlerMapping.producibleMediaTypes";

    /* Configuration Key */
    public static final String CONFIG_PREFIX = "dubbo.rpc.rest.";
    public static final String MAX_BODY_SIZE_KEY = CONFIG_PREFIX + "max-body-size";
    public static final String MAX_RESPONSE_BODY_SIZE_KEY = CONFIG_PREFIX + "max-response-body-size";
    public static final String SUFFIX_PATTERN_MATCH_KEY = CONFIG_PREFIX + "suffix-pattern-match";
    public static final String TRAILING_SLASH_MATCH_KEY = CONFIG_PREFIX + "trailing-slash-match";
    public static final String CASE_SENSITIVE_MATCH_KEY = CONFIG_PREFIX + "case-sensitive-match";
    public static final String FORMAT_PARAMETER_NAME_KEY = CONFIG_PREFIX + "format-parameter-name";

    /* Cors Configuration Key */
    public static final String CORS_CONFIG_PREFIX = CONFIG_PREFIX + "cors.";
    public static final String ALLOWED_ORIGINS = CORS_CONFIG_PREFIX + "allowed-origins";
    public static final String ALLOWED_METHODS = CORS_CONFIG_PREFIX + "allowed-methods";
    public static final String ALLOWED_HEADERS = CORS_CONFIG_PREFIX + "allowed-headers";
    public static final String ALLOW_CREDENTIALS = CORS_CONFIG_PREFIX + "allow-credentials";
    public static final String EXPOSED_HEADERS = CORS_CONFIG_PREFIX + "exposed-headers";
    public static final String MAX_AGE = CORS_CONFIG_PREFIX + "max-age";

    private RestConstants() {}
}
