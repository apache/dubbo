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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.nested.OpenAPIConfig;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Components;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Contact;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ExternalDocs;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Header;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Info;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.License;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.MediaType;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Node;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityRequirement;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityScheme;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Server;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Tag;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.apache.dubbo.rpc.protocol.tri.rest.openapi.Helper.setValue;

final class DefinitionMerger {

    private static final FluentLogger LOG = FluentLogger.of(DefinitionMerger.class);
    private static final String NAMING_STRATEGY_PREFIX = "naming-strategy-";
    private static final String NAMING_STRATEGY_DEFAULT = "default";
    private static Type SECURITY_SCHEMES_TYPE;
    private static Type SECURITY_TYPE;

    private final ExtensionFactory extensionFactory;
    private final ConfigFactory configFactory;
    private OpenAPINamingStrategy openAPINamingStrategy;

    DefinitionMerger(FrameworkModel frameworkModel) {
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        configFactory = frameworkModel.getOrRegisterBean(ConfigFactory.class);
    }

    private OpenAPINamingStrategy getNamingStrategy() {
        if (openAPINamingStrategy == null) {
            String strategy = configFactory.getGlobalConfig().getNameStrategy();
            String name = NAMING_STRATEGY_PREFIX + (strategy == null ? NAMING_STRATEGY_DEFAULT : strategy);
            openAPINamingStrategy = extensionFactory.getExtension(OpenAPINamingStrategy.class, name);
            Objects.requireNonNull(openAPINamingStrategy, "Can't find OpenAPINamingStrategy with name: " + name);
        }
        return openAPINamingStrategy;
    }

    public OpenAPI merge(List<OpenAPI> openAPIs, OpenAPIRequest request) {
        Info info = new Info();
        OpenAPI model = new OpenAPI().setInfo(info);

        OpenAPIConfig globalConfig = configFactory.getGlobalConfig();
        model.setGlobalConfig(globalConfig);
        applyConfig(model, globalConfig);
        if (openAPIs.isEmpty()) {
            return model;
        }

        String group = request.getGroup();
        String version = request.getVersion();
        String[] tags = request.getTag();
        String[] services = request.getService();

        if (group == null) {
            group = Constants.DEFAULT_GROUP;
        }
        model.setGroup(group);
        if (version != null) {
            info.setVersion(version);
        }
        model.setOpenapi(Helper.formatSpecVersion(request.getOpenapi()));

        OpenAPIConfig config = configFactory.getConfig(group);
        model.setConfig(config);

        for (OpenAPI api : openAPIs) {
            if (isServiceNotMatch(api.getMeta().getServiceInterface(), services)) {
                continue;
            }

            if (group.equals(api.getGroup())) {
                mergeBasic(model, api);
            }

            mergePaths(model, api, group, version, tags);

            mergeSecuritySchemes(model, api);

            mergeTags(model, api);
        }

        applyConfig(model, config);

        addSchemas(model, version, group);

        completeOperations(model);

        completeModel(model);

        return model;
    }

    private void applyConfig(OpenAPI api, OpenAPIConfig config) {
        if (config == null) {
            return;
        }

        Info info = api.getInfo();
        setValue(info::setTitle, config::getInfoTitle);
        setValue(info::setDescription, config::getInfoDescription);
        setValue(info::setVersion, config::getInfoVersion);

        Contact contact = info.getContact();
        if (contact == null) {
            info.setContact(contact = new Contact());
        }
        setValue(contact::setName, config::getInfoContactName);
        setValue(contact::setUrl, config::getInfoContactUrl);
        setValue(contact::setEmail, config::getInfoContactEmail);

        ExternalDocs externalDocs = api.getExternalDocs();
        if (externalDocs == null) {
            api.setExternalDocs(externalDocs = new ExternalDocs());
        }
        setValue(externalDocs::setDescription, config::getExternalDocsDescription);
        setValue(externalDocs::setUrl, config::getExternalDocsUrl);

        String[] servers = config.getServers();
        if (servers != null) {
            api.setServers(Arrays.stream(servers).map(Helper::parseServer).collect(Collectors.toList()));
        }

        Components components = api.getComponents();
        if (api.getComponents() == null) {
            api.setComponents(components = new Components());
        }

        String securityScheme = config.getSecurityScheme();
        if (securityScheme != null) {
            try {
                if (SECURITY_SCHEMES_TYPE == null) {
                    SECURITY_SCHEMES_TYPE =
                            Components.class.getDeclaredField("securitySchemes").getGenericType();
                }
                components.setSecuritySchemes(JsonUtils.toJavaObject(securityScheme, SECURITY_SCHEMES_TYPE));
            } catch (NoSuchFieldException ignored) {
            }
        }

        String security = config.getSecurity();
        if (security != null) {
            try {
                if (SECURITY_TYPE == null) {
                    SECURITY_TYPE = OpenAPI.class.getDeclaredField("security").getGenericType();
                }
                api.setSecurity(JsonUtils.toJavaObject(securityScheme, SECURITY_TYPE));
            } catch (NoSuchFieldException ignored) {
            }
        }
    }

    private void mergeBasic(OpenAPI api, OpenAPI from) {
        mergeInfo(api, from);

        if (api.getServers() == null) {
            api.setServers(Node.clone(from.getServers()));
        }

        List<SecurityRequirement> fromSecurity = from.getSecurity();
        if (api.getSecurity() == null) {
            api.setSecurity(Node.clone(fromSecurity));
        }

        ExternalDocs fromExternalDocs = from.getExternalDocs();
        if (fromExternalDocs != null) {
            ExternalDocs externalDocs = api.getExternalDocs();
            setValue(externalDocs::setDescription, fromExternalDocs::getDescription);
            setValue(externalDocs::setUrl, fromExternalDocs::getUrl);
            externalDocs.addExtensions(fromExternalDocs.getExtensions());
        }

        api.addExtensions(from.getExtensions());
    }

    private void mergeInfo(OpenAPI api, OpenAPI from) {
        Info fromInfo = from.getInfo();
        if (fromInfo == null) {
            return;
        }

        Info info = api.getInfo();
        setValue(info::setTitle, fromInfo::getTitle);
        setValue(info::setSummary, fromInfo::getSummary);
        setValue(info::setDescription, fromInfo::getDescription);
        setValue(info::setTermsOfService, fromInfo::getTermsOfService);
        setValue(info::setVersion, fromInfo::getVersion);

        Contact fromContact = fromInfo.getContact();
        if (fromContact != null) {
            Contact contact = info.getContact();
            setValue(contact::setName, fromContact::getName);
            setValue(contact::setUrl, fromContact::getUrl);
            setValue(contact::setEmail, fromContact::getEmail);

            contact.addExtensions(fromContact.getExtensions());
        }

        License fromLicense = fromInfo.getLicense();
        if (fromLicense != null) {
            License license = info.getLicense();
            setValue(license::setName, fromLicense::getName);
            setValue(license::setUrl, fromLicense::getUrl);
            license.addExtensions(fromLicense.getExtensions());
        }

        info.addExtensions(fromInfo.getExtensions());
    }

    private void mergePaths(OpenAPI api, OpenAPI from, String group, String version, String[] tags) {
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
            mergePath(path, pathItem, fromPathItem, group, version, tags);
        }
    }

    private void mergePath(String path, PathItem pathItem, PathItem from, String group, String version, String[] tags) {
        if (pathItem.getRef() == null) {
            pathItem.setRef(from.getRef());
        }
        if (pathItem.getSummary() == null) {
            pathItem.setSummary(from.getSummary());
        }
        if (pathItem.getDescription() == null) {
            pathItem.setDescription(from.getDescription());
        }

        Map<HttpMethods, Operation> fromOperations = from.getOperations();
        if (fromOperations != null) {
            for (Entry<HttpMethods, Operation> entry : fromOperations.entrySet()) {
                HttpMethods httpMethod = entry.getKey();
                Operation fromOperation = entry.getValue();
                if (isGroupNotMatch(group, fromOperation.getGroup())
                        || isVersionNotMatch(version, fromOperation.getVersion())
                        || isTagNotMatch(tags, fromOperation.getTags())) {
                    continue;
                }

                Operation operation = pathItem.getOperation(httpMethod);
                if (operation == null) {
                    pathItem.addOperation(httpMethod, fromOperation.clone());
                } else if (operation.getMeta() != null) {
                    LOG.internalWarn(
                            "Operation already exists, path='{}', httpMethod='{}', method={}",
                            path,
                            httpMethod,
                            fromOperation.getMeta());
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

    private static boolean isServiceNotMatch(String apiService, String[] services) {
        if (apiService == null || services == null) {
            return false;
        }
        for (String service : services) {
            if (apiService.regionMatches(true, 0, service, 0, service.length())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isGroupNotMatch(String group, String fromGroup) {
        return !(fromGroup == null && Constants.DEFAULT_GROUP.equals(group)
                || Constants.ALL_GROUP.equals(group)
                || group.equals(fromGroup));
    }

    private static boolean isVersionNotMatch(String version, String fromVersion) {
        return !(version == null || fromVersion == null || Helper.isVersionGreaterOrEqual(fromVersion, version));
    }

    private static boolean isTagNotMatch(String[] tags, Set<String> operationTags) {
        if (tags == null || operationTags == null) {
            return false;
        }
        for (String tag : tags) {
            if (operationTags.contains(tag)) {
                return false;
            }
        }
        return true;
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
        if (fromTags == null) {
            return;
        }

        if (api.getTags() == null) {
            api.setTags(Node.clone(fromTags));
        } else {
            for (Tag tag : fromTags) {
                api.addTag(tag.clone());
            }
        }
    }

    private void addSchemas(OpenAPI api, String version, String group) {
        Map<Schema, Schema> schemas = new IdentityHashMap<>();
        for (PathItem pathItem : api.getPaths().values()) {
            Map<HttpMethods, Operation> operations = pathItem.getOperations();
            if (operations == null) {
                continue;
            }
            for (Operation operation : operations.values()) {
                List<Parameter> parameters = operation.getParameters();
                if (parameters != null) {
                    for (Parameter parameter : parameters) {
                        addSchema(parameter.getSchema(), schemas, group, version);
                        Map<String, MediaType> contents = parameter.getContents();
                        if (contents == null) {
                            continue;
                        }
                        for (MediaType content : contents.values()) {
                            addSchema(content.getSchema(), schemas, group, version);
                        }
                    }
                }
                RequestBody requestBody = operation.getRequestBody();
                if (requestBody != null) {
                    Map<String, MediaType> contents = requestBody.getContents();
                    if (contents == null) {
                        continue;
                    }
                    for (MediaType content : contents.values()) {
                        addSchema(content.getSchema(), schemas, group, version);
                    }
                }
                Map<String, ApiResponse> responses = operation.getResponses();
                if (responses != null) {
                    for (ApiResponse response : responses.values()) {
                        Map<String, Header> headers = response.getHeaders();
                        if (headers != null) {
                            for (Header header : headers.values()) {
                                addSchema(header.getSchema(), schemas, group, version);
                            }
                        }

                        Map<String, MediaType> contents = response.getContents();
                        if (contents == null) {
                            continue;
                        }
                        for (MediaType content : contents.values()) {
                            addSchema(content.getSchema(), schemas, group, version);
                        }
                    }
                }
            }
        }

        Components components = api.getComponents();
        if (components == null) {
            api.setComponents(components = new Components());
        }

        Set<String> names = CollectionUtils.newHashSet(schemas.size());
        for (Schema schema : schemas.keySet()) {
            String name = schema.getName();
            if (name != null) {
                names.add(name);
            }
        }

        OpenAPINamingStrategy strategy = getNamingStrategy();
        for (Schema schema : schemas.values()) {
            String name = schema.getName();
            if (name == null) {
                Class<?> clazz = schema.getJavaType();
                name = strategy.generateSchemaName(clazz, api);
                for (int i = 1; i < 100; i++) {
                    if (names.contains(name)) {
                        name = strategy.resolveSchemaNameConflict(i, name, clazz, api);
                    } else {
                        names.add(name);
                        break;
                    }
                }
                schema.setName(name);
            }

            for (Schema sourceSchema : schema.getSourceSchemas()) {
                sourceSchema.setTargetSchema(schema);
                sourceSchema.setRef("#/components/schemas/" + name);
            }
            schema.setSourceSchemas(null);
            components.addSchema(name, schema);
        }
    }

    private void addSchema(Schema schema, Map<Schema, Schema> schemas, String group, String version) {
        if (schema == null) {
            return;
        }

        addSchema(schema.getItems(), schemas, group, version);

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            Iterator<Entry<String, Schema>> it = properties.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Schema> entry = it.next();
                Schema property = entry.getValue();
                if (isGroupNotMatch(group, property.getGroup()) || isVersionNotMatch(version, property.getVersion())) {
                    it.remove();
                    continue;
                }
                addSchema(property, schemas, group, version);
            }
        }

        addSchema(schema.getAdditionalPropertiesSchema(), schemas, group, version);

        List<Schema> allOf = schema.getAllOf();
        if (allOf != null) {
            for (Schema item : allOf) {
                addSchema(item, schemas, group, version);
            }
        }

        List<Schema> oneOf = schema.getOneOf();
        if (oneOf != null) {
            for (Schema item : oneOf) {
                addSchema(item, schemas, group, version);
            }
        }

        List<Schema> anyOf = schema.getAnyOf();
        if (anyOf != null) {
            for (Schema item : anyOf) {
                addSchema(item, schemas, group, version);
            }
        }

        addSchema(schema.getNot(), schemas, group, version);

        Schema targetSchema = schema.getTargetSchema();
        if (targetSchema == null) {
            return;
        }

        targetSchema.addSourceSchema(schema);

        schemas.computeIfAbsent(targetSchema, s -> {
            Schema newSchema = s.clone();
            addSchema(newSchema, schemas, group, version);
            return newSchema;
        });
    }

    private void completeOperations(OpenAPI api) {
        Map<String, PathItem> paths = api.getPaths();
        if (paths == null) {
            return;
        }

        Set<String> allOperationIds = new HashSet<>(32);
        Set<String> allTags = new HashSet<>(32);
        api.walkOperations(operation -> {
            String operationId = operation.getOperationId();
            if (operationId != null) {
                allOperationIds.add(operationId);
            }
            Set<String> tags = operation.getTags();
            if (tags != null) {
                allTags.addAll(tags);
            }
        });

        OpenAPINamingStrategy strategy = getNamingStrategy();
        api.walkOperations(operation -> {
            String id = operation.getOperationId();
            if (id != null) {
                return;
            }
            id = strategy.generateOperationId(operation.getMeta(), api);
            for (int i = 1; i < 100; i++) {
                if (allOperationIds.contains(id)) {
                    id = strategy.resolveOperationIdConflict(i, id, operation.getMeta(), api);
                } else {
                    allOperationIds.add(id);
                    break;
                }
            }
            operation.setOperationId(id);
        });

        List<Tag> tags = api.getTags();
        if (tags != null) {
            ListIterator<Tag> it = tags.listIterator();
            while (it.hasNext()) {
                if (allTags.contains(it.next().getName())) {
                    continue;
                }
                it.remove();
            }
        }
    }

    private void completeModel(OpenAPI api) {
        Info info = api.getInfo();
        if (info.getTitle() == null) {
            info.setTitle("Dubbo OpenAPI");
        }
        if (info.getVersion() == null) {
            info.setVersion("v1");
        }
        ExternalDocs docs = api.getExternalDocs();
        if (docs.getUrl() == null && docs.getDescription() == null) {
            docs.setUrl("../redoc/index.html?group=" + api.getGroup()).setDescription("ReDoc");
        }
    }
}
