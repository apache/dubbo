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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DefinitionMerger {

    private final ExtensionFactory extensionFactory;
    private final ConfigFactory configFactory;

    DefinitionMerger(FrameworkModel frameworkModel) {
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
    }

    public OpenAPI merge(List<OpenAPI> openAPIs, OpenAPIRequest request) {
        OpenAPI result = new OpenAPI();

        if (openAPIs.isEmpty()) {
            return result;
        }

        String group = trim(request.getGroup());
        if (group == null) {
            group = Constants.DEFAULT_GROUP;
        }
        String[] tagArray = trim(request.getTags());
        Set<String> tags = tagArray == null ? null : new HashSet<>(Arrays.asList(tagArray));
        String service = trim(request.getService());

        for (OpenAPI api : openAPIs) {

            result.setOpenapi(api.getOpenapi());
            result.setInfo(api.getInfo());
            result.setServers(api.getServers());
            result.setComponents(api.getComponents());
            result.setSecurity(api.getSecurity());
            result.setTags(api.getTags());
            result.setExternalDocs(api.getExternalDocs());
            result.setGroup(api.getGroup());
            result.setPriority(api.getPriority());
        }

        String version = request.getVersion();
        if (version == null) {
            version = result.getOpenapi();
        }
        if (version == null) {
            version = Constants.VERSION_30;
        } else {
            if (version.startsWith("3.0")) {
                version = Constants.VERSION_30;
            } else if (version.startsWith("3.1")) {
                version = Constants.VERSION_31;
            }
        }
        result.setOpenapi(version);

        return result;
    }

    private void mergeInfo(OpenAPI result, List<OpenAPI> openAPIs) {
        if (openAPIs.size() == 1) {
            result.setInfo(openAPIs.get(0).getInfo());
            return;
        }

        for (OpenAPI api : openAPIs) {
            if (api.getInfo() != null) {
                result.setInfo(api.getInfo());
                return;
            }
        }
    }

    public static String trim(String str) {
        return str == null || str.isEmpty() ? null : str.trim();
    }

    public static String[] trim(String[] array) {
        if (array == null) {
            return null;
        }
        int len = array.length;
        if (len == 0) {
            return null;
        }
        int p = 0;
        for (int i = 0; i < len; i++) {
            String value = trim(array[i]);
            if (value != null) {
                array[p++] = value;
            }
        }
        int newLen = p + 1;
        return newLen == len ? array : Arrays.copyOf(array, newLen);
    }
}
