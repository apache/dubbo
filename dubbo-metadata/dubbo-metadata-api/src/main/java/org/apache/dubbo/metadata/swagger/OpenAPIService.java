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

import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.OpenAPI;
import org.apache.dubbo.metadata.swagger.model.Paths;
import org.apache.dubbo.metadata.swagger.model.SpecVersion;
import org.apache.dubbo.metadata.swagger.model.info.Info;
import org.apache.dubbo.metadata.swagger.model.servers.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenAPIService {

    /**
     * The Cached open api map.
     */
    private final Map<String, OpenAPI> cachedOpenAPI = new HashMap<>();

    /**
     * The Is servers present.
     */
    private boolean isServersPresent;

    /**
     * The Server base url.
     */
    private final String serverBaseUrl = "http://localhost";

    /**
     * The constant DEFAULT_SERVER_DESCRIPTION.
     */
    public static final String DEFAULT_SERVER_DESCRIPTION = "Generated server url";

    static final String OPENAPI_3_0 = "3.0.1";

    /**
     * The constant DEFAULT_TITLE.
     */
    public static final String DEFAULT_TITLE = "OpenAPI definition";

    /**
     * The constant DEFAULT_VERSION.
     */
    public static final String DEFAULT_VERSION = "v0";

    /**
     * The Open api.
     */
    private OpenAPI openAPI;

    public OpenAPI getCachedOpenAPI(Locale locale) {
        return cachedOpenAPI.get(locale.toLanguageTag());
    }

    public void setCachedOpenAPI(OpenAPI openAPI, Locale locale) {
        cachedOpenAPI.put(locale.toLanguageTag(), openAPI);
    }

    public void updateServers(OpenAPI openAPI) {
        if (!isServersPresent) {
            Server server = new Server().url(serverBaseUrl).description(DEFAULT_SERVER_DESCRIPTION);
            List<Server> servers = new ArrayList<>();
            servers.add(server);
            openAPI.setServers(servers);
        }
    }

    public OpenAPI build(Locale locale) {
        OpenAPI calculatedOpenAPI = null;
        if (openAPI == null) {
            calculatedOpenAPI = new OpenAPI(SpecVersion.V30);
            calculatedOpenAPI.setComponents(new Components());
            calculatedOpenAPI.setPaths(new Paths());
        } else {

            calculatedOpenAPI = openAPI;
        }

        if (cachedOpenAPI != null && calculatedOpenAPI.getInfo() == null) {
            Info infos = new Info().title(DEFAULT_TITLE).version(DEFAULT_VERSION);
            calculatedOpenAPI.setInfo(infos);
        }

        return calculatedOpenAPI;
    }
}
