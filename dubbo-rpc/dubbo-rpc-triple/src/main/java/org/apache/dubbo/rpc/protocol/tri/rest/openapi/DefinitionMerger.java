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

import org.apache.dubbo.common.logger.FluentLogger;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Components;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Contact;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ExternalDocs;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.License;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Node;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityRequirement;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityScheme;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Server;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

final class DefinitionMerger {

    private static final FluentLogger LOG = FluentLogger.of(DefinitionMerger.class);

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
        String[] tags = trim(request.getTags());
        String service = trim(request.getService());
        String version = trim(request.getVersion());
        if (version != null) {
            result.setOpenapi(formatVersion(version));
        }

        applyConfig(result, configFactory.getConfig(group));

        for (OpenAPI openAPI : openAPIs) {
            if (service != null) {
                String serviceName = openAPI.getService().getServiceDescriptor().getInterfaceName();
                if (serviceName != null && !serviceName.regionMatches(true, 0, service, 0, service.length())) {
                    continue;
                }
            }

            if (group.equals(openAPI.getGroup())) {
                mergeBasic(result, openAPI);
            }

            mergePaths(result, openAPI, group, tags);

            mergeSecuritySchemes(result, openAPI);

            mergeTags(result, openAPI);
        }

        applyConfig(result, configFactory.getGlobalConfig());

        addRefSchemas(result);

        cleanup(result);

        return result;
    }

    private void applyConfig(OpenAPI api, OpenAPIConfig config) {
        if (config == null) {
            return;
        }

        if (api.getOpenapi() == null) {
            api.setOpenapi(formatVersion(config.getVersion()));
        }

        Info info = api.getInfo();
        if (info == null) {
            api.setInfo(info = new Info());
        }
        if (info.getTitle() == null) {
            info.setTitle(config.getInfoTitle());
        }
        if (info.getDescription() == null) {
            info.setDescription(config.getInfoDescription());
        }

        Contact contact = info.getContact();
        if (contact == null) {
            info.setContact(contact = new Contact());
        }
        if (contact.getName() == null) {
            contact.setName(config.getInfoContactName());
        }
        if (contact.getUrl() == null) {
            contact.setUrl(config.getInfoContactUrl());
        }
        if (contact.getEmail() == null) {
            contact.setEmail(config.getInfoContactEmail());
        }

        if (info.getVersion() == null) {
            info.setVersion(config.getInfoVersion());
        }

        ExternalDocs externalDocs = api.getExternalDocs();
        if (externalDocs == null) {
            api.setExternalDocs(externalDocs = new ExternalDocs());
        }
        if (externalDocs.getDescription() == null) {
            externalDocs.setDescription(config.getExternalDocsDescription());
        }
        if (externalDocs.getUrl() == null) {
            externalDocs.setUrl(config.getExternalDocsUrl());
        }

        if (api.getServers() == null) {
            String[] servers = config.getServers();
            if (servers != null) {
                for (String server : servers) {
                    api.addServer(new Server().setUrl(server));
                }
            }
        }

        Components components = api.getComponents();
        if (api.getComponents() == null) {
            api.setComponents(components = new Components());
        }
        if (components.getSecuritySchemes() == null) {
            String securityScheme = config.getSecurityScheme();
            if (securityScheme != null) {
                try {
                    Type type =
                            Components.class.getDeclaredField("securitySchemes").getGenericType();
                    components.setSecuritySchemes(JsonUtils.toJavaObject(securityScheme, type));
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
    }

    private void mergeBasic(OpenAPI api, OpenAPI from) {
        if (api.getOpenapi() == null) {
            api.setOpenapi(formatVersion(from.getOpenapi()));
        }

        mergeInfo(api, from);

        List<Server> fromServers = from.getServers();
        if (fromServers != null) {
            List<Server> servers = api.getServers();
            if (servers == null) {
                api.setServers(Node.clone(fromServers));
            }
        }

        List<SecurityRequirement> fromSecurity = from.getSecurity();
        if (fromSecurity != null) {
            List<SecurityRequirement> security = api.getSecurity();
            if (security == null) {
                api.setSecurity(Node.clone(fromSecurity));
            }
        }

        ExternalDocs fromExternalDocs = from.getExternalDocs();
        if (fromExternalDocs != null) {
            ExternalDocs externalDocs = api.getExternalDocs();
            if (externalDocs.getDescription() == null) {
                externalDocs.setDescription(fromExternalDocs.getDescription());
            }
            if (externalDocs.getUrl() == null) {
                externalDocs.setUrl(fromExternalDocs.getUrl());
            }
            externalDocs.addExtensions(fromExternalDocs.getExtensions());
        }

        api.addExtensions(from.getExtensions());
    }

    private void mergeInfo(OpenAPI api, OpenAPI from) {
        Info fromInfo = from.getInfo();
        if (fromInfo != null) {
            Info info = api.getInfo();
            if (info.getTitle() == null) {
                info.setTitle(fromInfo.getTitle());
            }
            if (info.getSummary() == null) {
                info.setDescription(fromInfo.getSummary());
            }
            if (info.getDescription() == null) {
                info.setDescription(fromInfo.getDescription());
            }
            if (info.getTermsOfService() == null) {
                info.setTermsOfService(fromInfo.getTermsOfService());
            }

            Contact fromContact = fromInfo.getContact();
            if (fromContact != null) {
                Contact contact = info.getContact();
                if (contact.getName() == null) {
                    contact.setName(fromContact.getName());
                }
                if (contact.getUrl() == null) {
                    contact.setUrl(fromContact.getUrl());
                }
                if (contact.getEmail() == null) {
                    contact.setEmail(fromContact.getEmail());
                }

                if (info.getVersion() == null) {
                    info.setVersion(fromInfo.getVersion());
                }
                contact.addExtensions(fromContact.getExtensions());
            }

            License fromLicense = fromInfo.getLicense();
            if (fromLicense != null) {
                License license = info.getLicense();
                if (license.getName() == null) {
                    license.setName(fromLicense.getName());
                }
                if (license.getUrl() == null) {
                    license.setUrl(fromLicense.getUrl());
                }
                license.addExtensions(fromLicense.getExtensions());
            }

            if (info.getVersion() == null) {
                info.setVersion(fromInfo.getVersion());
            }

            info.addExtensions(fromInfo.getExtensions());
        }
    }

    private void mergePaths(OpenAPI api, OpenAPI from, String group, String[] tags) {
        Map<String, PathItem> fromPaths = from.getPaths();
        if (fromPaths == null) {
            return;
        }

        Map<String, PathItem> paths = api.getPaths();
        if (paths == null) {
            api.setPaths(paths = new TreeMap<>());
        }

        for (Entry<String, PathItem> entry : fromPaths.entrySet()) {
            String path = entry.getKey();
            PathItem fromPathItem = entry.getValue();
            PathItem pathItem = paths.get(path);
            if (pathItem != null) {
                String ref = fromPathItem.getRef();
                if (ref != null) {
                    pathItem = paths.get(ref);
                }
            }
            if (pathItem == null) {
                paths.put(path, pathItem = new PathItem());
            }
            mergePathItem(path, pathItem, fromPathItem, group, tags);
        }
    }

    private void mergePathItem(String path, PathItem pathItem, PathItem from, String group, String[] tags) {
        if (pathItem.getSummary() == null) {
            pathItem.setSummary(from.getSummary());
        }
        if (pathItem.getDescription() == null) {
            pathItem.setDescription(from.getDescription());
        }

        Map<HttpMethods, Operation> fromOperations = from.getOperations();
        if (fromOperations != null) {
            Map<HttpMethods, Operation> operations = pathItem.getOperations();
            if (operations == null) {
                pathItem.setOperations(Node.clone(fromOperations));
            } else {
                for (Entry<HttpMethods, Operation> entry : fromOperations.entrySet()) {
                    HttpMethods httpMethod = entry.getKey();
                    Operation fromOperation = entry.getValue();
                    if (!group.equals(fromOperation.getGroup())) {
                        continue;
                    }

                    if (tags != null) {
                        Set<String> operationTags = fromOperation.getTags();
                        if (operationTags == null) {
                            continue;
                        }
                        boolean exclude = false;
                        for (String tag : tags) {
                            if (operationTags.contains(tag)) {
                                continue;
                            }
                            exclude = true;
                            break;
                        }
                        if (exclude) {
                            continue;
                        }
                    }

                    Operation operation = operations.get(httpMethod);
                    if (operation == null) {
                        operations.put(httpMethod, fromOperation.clone());
                    } else if (operation.getMethod() != null) {
                        LOG.internalWarn(
                                "Operation already exists, path='{}', httpMethod='{}', method={}",
                                path,
                                httpMethod,
                                fromOperation.getMethod());
                    }
                }
            }
        }

        if (pathItem.getServers() == null) {
            List<Server> fromServers = from.getServers();
            if (fromServers != null) {
                pathItem.setServers(Node.clone(fromServers));
            }
        }

        List<Parameter> fromParameters = from.getParameters();
        if (fromParameters != null) {
            if (pathItem.getParameters() == null) {
                pathItem.setParameters(Node.clone(fromParameters));
            } else {
                for (Parameter parameter : fromParameters) {
                    pathItem.addParameter(parameter.clone());
                }
            }
        }

        pathItem.addExtensions(from.getExtensions());
    }

    private void mergeSecuritySchemes(OpenAPI api, OpenAPI from) {
        Components fromComponents = from.getComponents();
        if (fromComponents == null) {
            return;
        }

        Map<String, SecurityScheme> fromSecuritySchemes = fromComponents.getSecuritySchemes();
        if (fromSecuritySchemes == null) {
            return;
        }

        Components components = api.getComponents();
        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
        if (securitySchemes == null) {
            components.setSecuritySchemes(Node.clone(fromSecuritySchemes));
        } else {
            for (Entry<String, SecurityScheme> entry : fromSecuritySchemes.entrySet()) {
                String key = entry.getKey();
                if (securitySchemes.containsKey(key)) {
                    continue;
                }
                securitySchemes.put(key, entry.getValue().clone());
            }
        }
    }

    private void mergeTags(OpenAPI api, OpenAPI from) {
        List<Tag> fromTags = from.getTags();
        if (fromTags != null) {
            if (api.getTags() == null) {
                api.setTags(Node.clone(fromTags));
            } else {
                for (Tag tag : fromTags) {
                    api.addTag(tag.clone());
                }
            }
        }
    }

    private void addRefSchemas(OpenAPI api) {}

    private void cleanup(OpenAPI api) {}

    private static String formatVersion(String version) {
        if (version == null) {
            return null;
        }
        if (version.startsWith("3.1")) {
            return Constants.VERSION_31;
        }
        return Constants.VERSION_30;
    }

    private static String trim(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        str = str.trim();
        return str.isEmpty() ? null : str;
    }

    private static String[] trim(String[] array) {
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
