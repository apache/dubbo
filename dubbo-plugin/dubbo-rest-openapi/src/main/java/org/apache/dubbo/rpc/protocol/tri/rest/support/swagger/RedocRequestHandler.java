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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.ConfigFactory;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Constants;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.OpenAPIRequestHandler;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Activate
public class RedocRequestHandler implements OpenAPIRequestHandler {

    private static final String DEFAULT_CDN = "https://cdn.redoc.ly/redoc/latest/bundles";
    private static final String INDEX_PATH = "/META-INF/resources/redoc/index.html";

    private final ConfigFactory configFactory;

    private OpenAPIConfig config;

    public RedocRequestHandler(FrameworkModel frameworkModel) {
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
        return new String[] {"/redoc/{*path}"};
    }

    @Override
    public HttpResult<?> handle(String path, HttpRequest request, HttpResponse response) {
        String resPath = RequestUtils.getPathVariable(request, "path");
        if (StringUtils.isEmpty(resPath)) {
            throw HttpResult.found(PathUtils.join(request.path(), "index.html")).toPayload();
        }
        String requestPath = StringUtils.substringBeforeLast(resPath, '.');
        if (requestPath.equals("index")) {
            return handleIndex(request.parameter("group", Constants.DEFAULT_GROUP));
        } else if (WebjarHelper.ENABLED && requestPath.startsWith("assets/")) {
            return WebjarHelper.getInstance().handleAssets("redoc", resPath.substring(7));
        }
        throw new HttpStatusException(HttpStatus.NOT_FOUND.getCode());
    }

    private HttpResult<?> handleIndex(String group) {
        Map<String, String> variables = new HashMap<>(4);

        OpenAPIConfig config = getConfig();
        String cdn = config.getSetting("redoc.cdn");
        if (cdn == null) {
            if (WebjarHelper.ENABLED && WebjarHelper.getInstance().hasWebjar("redoc")) {
                cdn = "./assets";
            } else {
                cdn = DEFAULT_CDN;
            }
        }
        variables.put("redoc.cdn", cdn);
        variables.put("group", group);
        try {
            String content = StreamUtils.toString(getClass().getResourceAsStream(INDEX_PATH));
            return HttpResult.of(Helper.render(content, variables::get).getBytes(UTF_8));
        } catch (IOException e) {
            throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), e);
        }
    }
}
