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
package org.apache.dubbo.metadata.swagger;

public class SwaggerConstants {
    public static final String DEFAULT_CONSUMES_MEDIA_TYPE = "application/json";
    public static final String DEFAULT_PRODUCES_MEDIA_TYPE = "*/*";

    public static final String DEFAULT = "";
    public static final String HEADER = "header";
    public static final String QUERY = "query";
    public static final String PATH = "path";
    public static final String COOKIE = "cookie";

    /**
     * The constant DEFAULT_TITLE.
     */
    public static final String DEFAULT_TITLE = "OpenAPI definition";

    /**
     * The constant DEFAULT_VERSION.
     */
    public static final String DEFAULT_VERSION = "v0";

    /**
     * The constant DEFAULT_SERVER_DESCRIPTION.
     */
    public static final String DEFAULT_SERVER_DESCRIPTION = "Generated server url";

    public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

    public static final String DEFAULT_NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";

    public static final String DEFAULT_DESCRIPTION = "default response";

    public static final String COMPONENTS_REF = "#/components/schemas/";
}
