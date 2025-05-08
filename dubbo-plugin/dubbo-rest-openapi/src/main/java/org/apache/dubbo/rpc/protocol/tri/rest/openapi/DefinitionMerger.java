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
        OpenAPI target = new OpenAPI().setInfo(info);

        OpenAPIConfig globalConfig = configFactory.getGlobalConfig();
        target.setGlobalConfig(globalConfig);
        applyConfig(target, globalConfig);
        if (openAPIs.isEmpty()) {
            return target;
        }

        String group = request.getGroup();
        if (group == null) {
            group = Constants.DEFAULT_GROUP;
        }
        target.setGroup(group);

        String version = request.getVersion();
        if (version != null) {
            info.setVersion(version);
        }
        target.setOpenapi(Helper.formatSpecVersion(request.getOpenapi()));

        OpenAPIConfig config = configFactory.getConfig(group);
        target.setConfig(config);

        String[] tags = request.getTag();
        String[] services = request.getService();
        for (int i = openAPIs.size() - 1; i >= 0; i--) {
            OpenAPI source = openAPIs.get(i);
            if (isServiceNotMatch(source.getMeta().getServiceInterface(), services)) {
                continue;
            }

            if (group.equals(source.getGroup())) {
                mergeBasic(target, source);
            }

            mergePaths(target, source, group, version, tags);

            mergeSecuritySchemes(target, source);

            mergeTags(target, source);
        }

        applyConfig(target, config);

        addSchemas(target, version, group);

        completeOperations(target);

        completeModel(target);

        return target;
    }

    private void applyConfig(OpenAPI target, OpenAPIConfig config) {
        if (config == null) {
            return;
        }

        Info info = target.getInfo();
        setValue(config::getInfoTitle, info::setTitle);
        setValue(config::getInfoDescription, info::setDescription);
        setValue(config::getInfoVersion, info::setVersion);

        Contact contact = info.getContact();
        if (contact == null) {
            info.setContact(contact = new Contact());
        }
        setValue(config::getInfoContactName, contact::setName);
        setValue(config::getInfoContactUrl, contact::setUrl);
        setValue(config::getInfoContactEmail, contact::setEmail);

        ExternalDocs externalDocs = target.getExternalDocs();
        if (externalDocs == null) {
            target.setExternalDocs(externalDocs = new ExternalDocs());
        }
        setValue(config::getExternalDocsDescription, externalDocs::setDescription);
        setValue(config::getExternalDocsUrl, externalDocs::setUrl);

        String[] servers = config.getServers();
        if (servers != null) {
            target.setServers(Arrays.stream(servers).map(Helper::parseServer).collect(Collectors.toList()));
        }

        Components components = target.getComponents();
        if (target.getComponents() == null) {
            target.setComponents(components = new Components());
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
                target.setSecurity(JsonUtils.toJavaObject(securityScheme, SECURITY_TYPE));
            } catch (NoSuchFieldException ignored) {
            }
        }
    }

    private void mergeBasic(OpenAPI target, OpenAPI source) {
        mergeInfo(target, source);

        if (target.getServers() == null) {
            target.setServers(Node.clone(source.getServers()));
        }

        List<SecurityRequirement> sourceSecurity = source.getSecurity();
        if (target.getSecurity() == null) {
            target.setSecurity(Node.clone(sourceSecurity));
        }

        ExternalDocs sourceExternalDocs = source.getExternalDocs();
        if (sourceExternalDocs != null) {
            ExternalDocs targetExternalDocs = target.getExternalDocs();
            setValue(sourceExternalDocs::getDescription, targetExternalDocs::setDescription);
            setValue(sourceExternalDocs::getUrl, targetExternalDocs::setUrl);
            targetExternalDocs.addExtensions(sourceExternalDocs.getExtensions());
        }

        target.addExtensions(source.getExtensions());
    }

    private void mergeInfo(OpenAPI target, OpenAPI source) {
        Info sourceInfo = source.getInfo();
        if (sourceInfo == null) {
            return;
        }

        Info info = target.getInfo();
        setValue(sourceInfo::getTitle, info::setTitle);
        setValue(sourceInfo::getSummary, info::setSummary);
        setValue(sourceInfo::getDescription, info::setDescription);
        setValue(sourceInfo::getTermsOfService, info::setTermsOfService);
        setValue(sourceInfo::getVersion, info::setVersion);

        Contact sourceContact = sourceInfo.getContact();
        if (sourceContact != null) {
            Contact contact = info.getContact();
            setValue(sourceContact::getName, contact::setName);
            setValue(sourceContact::getUrl, contact::setUrl);
            setValue(sourceContact::getEmail, contact::setEmail);

            contact.addExtensions(sourceContact.getExtensions());
        }

        License sourceLicense = sourceInfo.getLicense();
        if (sourceLicense != null) {
            License license = info.getLicense();
            if (license == null) {
                info.setLicense(license = new License());
            }
            setValue(sourceLicense::getName, license::setName);
            setValue(sourceLicense::getUrl, license::setUrl);
            license.addExtensions(sourceLicense.getExtensions());
        }

        info.addExtensions(sourceInfo.getExtensions());
    }

    private void mergePaths(OpenAPI target, OpenAPI source, String group, String version, String[] tags) {
        Map<String, PathItem> sourcePaths = source.getPaths();
        if (sourcePaths == null) {
            return;
        }

        Map<String, PathItem> paths = target.getPaths();
        if (paths == null) {
            target.setPaths(paths = new TreeMap<>());
        }

        for (Entry<String, PathItem> entry : sourcePaths.entrySet()) {
            String path = entry.getKey();
            PathItem sourcePathItem = entry.getValue();
            PathItem pathItem = paths.get(path);
            if (pathItem != null) {
                String ref = sourcePathItem.getRef();
                if (ref != null) {
                    pathItem = paths.get(ref);
                }
            }
            if (pathItem == null) {
                paths.put(path, pathItem = new PathItem());
            }
            mergePath(path, pathItem, sourcePathItem, group, version, tags);
        }
    }

    private void mergePath(String path, PathItem target, PathItem source, String group, String version, String[] tags) {
        if (target.getRef() == null) {
            target.setRef(source.getRef());
        }
        if (target.getSummary() == null) {
            target.setSummary(source.getSummary());
        }
        if (target.getDescription() == null) {
            target.setDescription(source.getDescription());
        }

        Map<HttpMethods, Operation> sourceOperations = source.getOperations();
        if (sourceOperations != null) {
            for (Entry<HttpMethods, Operation> entry : sourceOperations.entrySet()) {
                HttpMethods httpMethod = entry.getKey();
                Operation sourceOperation = entry.getValue();
                if (isGroupNotMatch(group, sourceOperation.getGroup())
                        || isVersionNotMatch(version, sourceOperation.getVersion())
                        || isTagNotMatch(tags, sourceOperation.getTags())) {
                    continue;
                }

                Operation operation = target.getOperation(httpMethod);
                if (operation == null) {
                    target.addOperation(httpMethod, sourceOperation.clone());
                } else if (operation.getMeta() != null) {
                    LOG.internalWarn(
                            "Operation already exists, path='{}', httpMethod='{}', method={}",
                            path,
                            httpMethod,
                            sourceOperation.getMeta());
                }
            }
        }

        if (target.getServers() == null) {
            List<Server> sourceServers = source.getServers();
            if (sourceServers != null) {
                target.setServers(Node.clone(sourceServers));
            }
        }

        List<Parameter> sourceParameters = source.getParameters();
        if (sourceParameters != null) {
            if (target.getParameters() == null) {
                target.setParameters(Node.clone(sourceParameters));
            } else {
                for (Parameter parameter : sourceParameters) {
                    target.addParameter(parameter.clone());
                }
            }
        }

        target.addExtensions(source.getExtensions());
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

    private static boolean isGroupNotMatch(String group, String sourceGroup) {
        return !(sourceGroup == null && Constants.DEFAULT_GROUP.equals(group)
                || Constants.ALL_GROUP.equals(group)
                || group.equals(sourceGroup));
    }

    private static boolean isVersionNotMatch(String version, String sourceVersion) {
        return !(version == null || sourceVersion == null || Helper.isVersionGreaterOrEqual(sourceVersion, version));
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

    private void mergeSecuritySchemes(OpenAPI target, OpenAPI source) {
        Components sourceComponents = source.getComponents();
        if (sourceComponents == null) {
            return;
        }

        Map<String, SecurityScheme> sourceSecuritySchemes = sourceComponents.getSecuritySchemes();
        if (sourceSecuritySchemes == null) {
            return;
        }

        Components components = target.getComponents();
        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
        if (securitySchemes == null) {
            components.setSecuritySchemes(Node.clone(sourceSecuritySchemes));
        } else {
            for (Entry<String, SecurityScheme> entry : sourceSecuritySchemes.entrySet()) {
                securitySchemes.computeIfAbsent(
                        entry.getKey(), k -> entry.getValue().clone());
            }
        }
    }

    private void mergeTags(OpenAPI target, OpenAPI source) {
        List<Tag> sourceTags = source.getTags();
        if (sourceTags == null) {
            return;
        }

        if (target.getTags() == null) {
            target.setTags(Node.clone(sourceTags));
        } else {
            for (Tag tag : sourceTags) {
                target.addTag(tag.clone());
            }
        }
    }

    private void addSchemas(OpenAPI target, String version, String group) {
        Map<String, PathItem> paths = target.getPaths();
        if (paths == null) {
            return;
        }
        Map<Schema, Schema> schemas = new IdentityHashMap<>();
        for (PathItem pathItem : paths.values()) {
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

        Components components = target.getComponents();
        if (components == null) {
            target.setComponents(components = new Components());
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
                name = strategy.generateSchemaName(clazz, target);
                for (int i = 1; i < 100; i++) {
                    if (names.contains(name)) {
                        name = strategy.resolveSchemaNameConflict(i, name, clazz, target);
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

        Schema newSchema = schemas.get(targetSchema);
        if (newSchema == null) {
            newSchema = targetSchema.clone();
            schemas.put(targetSchema, newSchema);
            addSchema(newSchema, schemas, group, version);
        }
    }

    private void completeOperations(OpenAPI target) {
        Map<String, PathItem> paths = target.getPaths();
        if (paths == null) {
            return;
        }

        Set<String> allOperationIds = new HashSet<>(32);
        Set<String> allTags = new HashSet<>(32);
        target.walkOperations(operation -> {
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
        target.walkOperations(operation -> {
            String id = operation.getOperationId();
            if (id != null) {
                return;
            }
            id = strategy.generateOperationId(operation.getMeta(), target);
            for (int i = 1; i < 100; i++) {
                if (allOperationIds.contains(id)) {
                    id = strategy.resolveOperationIdConflict(i, id, operation.getMeta(), target);
                } else {
                    allOperationIds.add(id);
                    break;
                }
            }
            operation.setOperationId(id);
        });

        List<Tag> tags = target.getTags();
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

    private void completeModel(OpenAPI target) {
        Info info = target.getInfo();
        if (info.getTitle() == null) {
            info.setTitle("Dubbo OpenAPI");
        }
        if (info.getVersion() == null) {
            info.setVersion("v1");
        }
        if (CollectionUtils.isEmptyMap(target.getPaths())) {
            return;
        }
        ExternalDocs docs = target.getExternalDocs();
        if (docs.getUrl() == null && docs.getDescription() == null) {
            docs.setUrl("../redoc/index.html?group=" + target.getGroup()).setDescription("ReDoc");
        }
    }
}
