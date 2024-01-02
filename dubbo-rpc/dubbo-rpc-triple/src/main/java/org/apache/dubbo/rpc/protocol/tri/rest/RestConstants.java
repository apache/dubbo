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

public final class RestConstants {

    public static final String REST = "rest";

    public static final String REST_FILTER_KEY = "rest.filter";
    public static final String EXTENSION_KEY = "extension";
    public static final String EXTENSIONS_ATTRIBUTE_KEY = "restExtensionsAttributeKey";

    public static final String SLASH = "/";

    public static final String PATH_ATTRIBUTE = "org.springframework.web.util.UrlPathHelper.PATH";
    public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = "org.springframework.web.util.uriTemplateVariables";
    public static final String MATRIX_VARIABLES_ATTRIBUTE = "org.springframework.web.util.matrixVariables";
    public static final String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = "org.springframework.web.util.producibleMediaTypes";

    private RestConstants() {}
}
