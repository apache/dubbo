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
package org.apache.dubbo.rpc.protocol.tri.rest.support.swagger;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.rest.OpenAPIService;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.ConfigFactory;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIRequestHandler;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Activate
public class SwaggerUIRequestHandler implements OpenAPIRequestHandler {

    private static final String DEFAULT_CDN = "https://unpkg.com/swagger-ui-dist@5.18.2";
    private static final String INDEX_PATH = "/META-INF/resources/swagger-ui/index.html";

    private final FrameworkModel frameworkModel;
    private final ConfigFactory configFactory;

    private OpenAPIConfig config;

    public SwaggerUIRequestHandler(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
    }

    private OpenAPIConfig getConfig() {
        if (config == null) {
            config = configFactory.getGlobalConfig();
        }
        return config;
    }

    @Override
    public String[] getPaths() {
        return new String[] {"/swagger-ui/{*path}"};
    }

    @Override
    public HttpResult<?> handle(String path, HttpRequest request, HttpResponse response) {
        String resPath = RequestUtils.getPathVariable(request, "path");
        if (StringUtils.isEmpty(resPath)) {
            throw HttpResult.found(PathUtils.join(request.uri(), "index.html")).toPayload();
        }
        String requestPath = StringUtils.substringBeforeLast(resPath, '.');
        switch (requestPath) {
            case "index":
                return handleIndex();
            case "swagger-config":
                return handleSwaggerConfig();
            default:
                if (WebjarHelper.ENABLED && requestPath.startsWith("assets/")) {
                    return WebjarHelper.getInstance().handleAssets("swagger-ui", resPath.substring(7));
                }
        }
        throw new HttpStatusException(HttpStatus.NOT_FOUND.getCode());
    }

    private HttpResult<?> handleIndex() {
        Map<String, String> variables = new HashMap<>(4);

        OpenAPIConfig config = getConfig();
        String cdn = config.getSetting("swagger-ui.cdn");
        if (cdn == null) {
            if (WebjarHelper.ENABLED && WebjarHelper.getInstance().hasWebjar("swagger-ui")) {
                cdn = "./assets";
            } else {
                cdn = DEFAULT_CDN;
            }
        }
        variables.put("swagger-ui.cdn", cdn);

        Map<String, String> settings = config.getSettings();
        if (settings != null) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("swagger-ui.settings.")) {
                    sb.append(",\n            \"")
                            .append(key.substring(20))
                            .append("\": ")
                            .append(entry.getValue());
                }
            }
            if (sb.length() > 0) {
                variables.put("swagger-ui.settings", sb.toString());
            }
        }

        try {
            String content = StreamUtils.toString(getClass().getResourceAsStream(INDEX_PATH));
            return HttpResult.of(Helper.render(content, variables::get).getBytes(UTF_8));
        } catch (IOException e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), e);
        }
    }

    private HttpResult<?> handleSwaggerConfig() {
        OpenAPIService openAPIService = frameworkModel.getBean(OpenAPIService.class);
        if (openAPIService == null) {
            return HttpResult.notFound();
        }
        Collection<String> groups = openAPIService.getOpenAPIGroups();
        List<Map<String, String>> urls = new ArrayList<>();
        for (String group : groups) {
            Map<String, String> url = new LinkedHashMap<>(4);
            url.put("name", group);
            url.put("url", "../api-docs/" + group);
            urls.add(url);
        }

        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("urls", urls);
        return HttpResult.of(JsonUtils.toJson(configMap).getBytes(UTF_8));
    }
}
