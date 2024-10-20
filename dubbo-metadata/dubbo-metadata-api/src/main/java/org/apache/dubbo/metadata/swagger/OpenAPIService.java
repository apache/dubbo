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

import org.apache.dubbo.common.json.impl.JacksonImpl;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.swagger.model.Components;
import org.apache.dubbo.metadata.swagger.model.OpenAPI;
import org.apache.dubbo.metadata.swagger.model.Paths;
import org.apache.dubbo.metadata.swagger.model.info.Info;
import org.apache.dubbo.metadata.swagger.model.servers.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonParseException;

import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_SERVER_DESCRIPTION;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_TITLE;
import static org.apache.dubbo.metadata.swagger.SwaggerConstants.DEFAULT_VERSION;

public class OpenAPIService {

    Logger LOGGER = LoggerFactory.getLogger(OpenAPI.class);
    /**
     * The Cached open api map.
     */
    private final Map<String, OpenAPI> cachedOpenAPI = new HashMap<>();

    /**
     * The Server base url.
     */
    private String serverBaseUrl;

    private OpenAPI openAPI;

    public OpenAPI getCachedOpenAPI(Locale locale) {
        return cachedOpenAPI.get(locale.toLanguageTag());
    }

    public void setCachedOpenAPI(OpenAPI cachedOpenAPI, Locale locale) {
        this.cachedOpenAPI.put(locale.toLanguageTag(), cachedOpenAPI);
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public void setServersPresent(boolean serversPresent) {
        isServersPresent = serversPresent;
    }

    private boolean isServersPresent;

    public OpenAPIService(OpenAPI openAPI) {
        if (openAPI != null) {
            this.openAPI = openAPI;
            if (this.openAPI.getComponents() == null) this.openAPI.setComponents(new Components());
            if (this.openAPI.getPaths() == null) this.openAPI.setPaths(new Paths());
            if (!CollectionUtils.isEmpty(this.openAPI.getServers())) this.isServersPresent = true;
        }
    }

    public OpenAPI build() {
        OpenAPI calculatedOpenAPI = null;
        if (openAPI == null) {
            calculatedOpenAPI = new OpenAPI();
            calculatedOpenAPI.setComponents(new Components());
            calculatedOpenAPI.setPaths(new Paths());
        } else {
            try {
                JacksonImpl jackson = new JacksonImpl();
                String jsonString = jackson.toJson(openAPI);
                calculatedOpenAPI = jackson.toJavaObject(jsonString, OpenAPI.class);
            } catch (JsonParseException e) {
                LOGGER.warn("Json Processing Exception occurred: {}", e.getMessage());
                calculatedOpenAPI = openAPI;
            }
        }

        if (calculatedOpenAPI != null && calculatedOpenAPI.getInfo() == null) {
            Info infos = new Info().title(DEFAULT_TITLE).version(DEFAULT_VERSION);
            calculatedOpenAPI.setInfo(infos);
        }

        return calculatedOpenAPI;
    }

    public void updateServers(OpenAPI openAPI) {
        if (serverBaseUrl != null) {
            Server server = new Server().url(serverBaseUrl).description(DEFAULT_SERVER_DESCRIPTION);
            List<Server> servers = new ArrayList<>();
            servers.add(server);
            openAPI.setServers(servers);
        }
    }
}
