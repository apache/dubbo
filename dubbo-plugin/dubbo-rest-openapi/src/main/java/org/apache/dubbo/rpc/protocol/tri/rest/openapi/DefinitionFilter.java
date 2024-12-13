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

import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.ApiResponse;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Components;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Header;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.MediaType;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Node;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Operation;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Parameter;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.PathItem;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.RequestBody;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Schema;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.SecurityScheme;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.Server;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DefinitionFilter {

    private final ExtensionFactory extensionFactory;
    private final SchemaResolver schemaResolver;

    public DefinitionFilter(FrameworkModel frameworkModel) {
        extensionFactory = frameworkModel.getOrRegisterBean(ExtensionFactory.class);
        schemaResolver = frameworkModel.getOrRegisterBean(SchemaResolver.class);
    }

    public OpenAPI filter(OpenAPI openAPI, OpenAPIRequest request) {
        OpenAPIFilter[] filters = extensionFactory.getExtensions(OpenAPIFilter.class, request.getGroup());
        Context context = new ContextImpl(openAPI, schemaResolver, extensionFactory, request);

        if (filters.length > 0) {
            for (OpenAPIFilter filter : filters) {
                openAPI = filter.filterOpenAPI(openAPI, context);
                if (openAPI == null) {
                    return null;
                }
            }

            filterPaths(openAPI, filters, context);

            filterComponents(openAPI, filters, context);

            for (OpenAPIFilter filter : filters) {
                openAPI = filter.filterOpenAPICompletion(openAPI, context);
                if (openAPI == null) {
                    return null;
                }
            }
        }

        filterServer(openAPI, context);

        return openAPI;
    }

    private static void filterServer(OpenAPI openAPI, Context context) {
        List<Server> servers = openAPI.getServers();
        if (servers == null || servers.size() != 1) {
            return;
        }
        Server server = servers.get(0);
        if (!Constants.DUBBO_DEFAULT_SERVER.equals(server.getDescription())) {
            return;
        }
        HttpRequest httpRequest = context.getHttpRequest();
        if (httpRequest == null) {
            return;
        }
        String host = httpRequest.serverHost();
        if (host == null) {
            return;
        }
        String referer = httpRequest.header(Constants.REFERER);
        if (referer != null && referer.contains(host)) {
            servers.clear();
        } else {
            server.setUrl(httpRequest.scheme() + "://" + host);
        }
    }

    private void filterPaths(OpenAPI openAPI, OpenAPIFilter[] filters, Context context) {
        Map<String, PathItem> paths = openAPI.getPaths();
        if (paths == null) {
            return;
        }

        Iterator<Entry<String, PathItem>> it = paths.entrySet().iterator();
        out:
        while (it.hasNext()) {
            Entry<String, PathItem> entry = it.next();
            PathItem pathItem = entry.getValue();
            PathItem initialPathItem = pathItem;
            for (OpenAPIFilter filter : filters) {
                pathItem = filter.filterPathItem(entry.getKey(), pathItem, context);
                if (pathItem == null) {
                    it.remove();
                    continue out;
                }
            }
            if (pathItem != initialPathItem) {
                entry.setValue(pathItem);
            }

            filterOperation(pathItem, filters, context);
        }
    }

    private void filterOperation(PathItem pathItem, OpenAPIFilter[] filters, Context context) {
        Map<HttpMethods, Operation> operations = pathItem.getOperations();
        if (operations == null) {
            return;
        }

        Iterator<Entry<HttpMethods, Operation>> it = operations.entrySet().iterator();
        out:
        while (it.hasNext()) {
            Entry<HttpMethods, Operation> entry = it.next();
            HttpMethods httpMethod = entry.getKey();
            Operation operation = entry.getValue();
            Operation initialOperation = operation;
            for (OpenAPIFilter filter : filters) {
                operation = filter.filterOperation(httpMethod, operation, pathItem, context);
                if (operation == null) {
                    it.remove();
                    continue out;
                }
            }
            if (operation != initialOperation) {
                entry.setValue(operation);
            }

            filterParameter(operation, filters, context);
            filterRequestBody(operation, filters, context);
            filterResponse(operation, filters, context);
        }
    }

    private void filterParameter(Operation operation, OpenAPIFilter[] filters, Context context) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return;
        }

        ListIterator<Parameter> it = parameters.listIterator();
        out:
        while (it.hasNext()) {
            Parameter parameter = it.next();
            Parameter initialParameter = parameter;
            for (OpenAPIFilter filter : filters) {
                parameter = filter.filterParameter(parameter, operation, context);
                if (parameter == null) {
                    it.remove();
                    continue out;
                }
            }
            if (parameter != initialParameter) {
                it.set(parameter);
            }

            filterContext(parameter.getContents(), filters, context);
        }
    }

    private void filterRequestBody(Operation operation, OpenAPIFilter[] filters, Context context) {
        RequestBody body = operation.getRequestBody();
        if (body == null) {
            return;
        }

        RequestBody initialRequestBody = body;
        for (OpenAPIFilter filter : filters) {
            body = filter.filterRequestBody(body, operation, context);
            if (body == null) {
                operation.setRequestBody(null);
                return;
            }
        }
        if (body != initialRequestBody) {
            operation.setRequestBody(body);
        }

        filterContext(body.getContents(), filters, context);
    }

    private void filterResponse(Operation operation, OpenAPIFilter[] filters, Context context) {
        Map<String, ApiResponse> responses = operation.getResponses();
        if (responses == null) {
            return;
        }

        Iterator<Entry<String, ApiResponse>> it = responses.entrySet().iterator();
        out:
        while (it.hasNext()) {
            Entry<String, ApiResponse> entry = it.next();
            ApiResponse response = entry.getValue();
            ApiResponse initialApiResponse = response;
            for (OpenAPIFilter filter : filters) {
                response = filter.filterResponse(response, operation, context);
                if (response == null) {
                    it.remove();
                    continue out;
                }
            }
            if (response != initialApiResponse) {
                entry.setValue(response);
            }

            filterHeader(response, operation, filters, context);
            filterContext(response.getContents(), filters, context);
        }
    }

    private void filterHeader(ApiResponse response, Operation operation, OpenAPIFilter[] filters, Context context) {
        Map<String, Header> headers = response.getHeaders();
        if (headers == null) {
            return;
        }

        Iterator<Entry<String, Header>> it = headers.entrySet().iterator();
        out:
        while (it.hasNext()) {
            Entry<String, Header> entry = it.next();
            Header header = entry.getValue();
            Header initialHeader = header;
            for (OpenAPIFilter filter : filters) {
                header = filter.filterHeader(header, response, operation, context);
                if (header == null) {
                    it.remove();
                    continue out;
                }
            }
            if (header != initialHeader) {
                entry.setValue(header);
            }

            filterSchema(header::getSchema, header::setSchema, header, filters, context);

            Map<String, MediaType> contents = header.getContents();
            if (contents == null) {
                continue;
            }

            for (MediaType content : contents.values()) {
                filterSchema(content::getSchema, content::setSchema, content, filters, context);
            }
        }
    }

    private boolean filterContext(Map<String, MediaType> contents, OpenAPIFilter[] filters, Context context) {
        if (contents == null) {
            return true;
        }

        for (MediaType content : contents.values()) {
            filterSchema(content::getSchema, content::setSchema, content, filters, context);
        }
        return false;
    }

    private void filterComponents(OpenAPI openAPI, OpenAPIFilter[] filters, Context context) {
        Components components = openAPI.getComponents();
        if (components == null) {
            return;
        }

        filterSchemas(components, filters, context);
        filterSecuritySchemes(components, filters, context);
    }

    private void filterSchemas(Components components, OpenAPIFilter[] filters, Context context) {
        if (components == null) {
            return;
        }

        Map<String, Schema> schemas = components.getSchemas();
        if (schemas == null) {
            return;
        }

        for (Entry<String, Schema> entry : schemas.entrySet()) {
            filterSchema(entry::getValue, entry::setValue, components, filters, context);
        }
    }

    private void filterSchema(
            Supplier<Schema> getter, Consumer<Schema> setter, Node<?> owner, OpenAPIFilter[] filters, Context context) {
        Schema schema = getter.get();
        if (schema == null) {
            return;
        }

        Schema initialSchema = schema;
        for (OpenAPIFilter filter : filters) {
            schema = filter.filterSchema(schema, owner, context);
            if (schema == null) {
                setter.accept(null);
                return;
            }
        }
        if (schema != initialSchema) {
            setter.accept(schema);
        }

        filterSchema(schema::getItems, schema::setItems, schema, filters, context);

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            out:
            for (Entry<String, Schema> entry : properties.entrySet()) {
                String name = entry.getKey();
                Schema valueSchema = entry.getValue();
                for (OpenAPIFilter filter : filters) {
                    valueSchema = filter.filterSchemaProperty(name, valueSchema, schema, context);
                    if (valueSchema == null) {
                        entry.setValue(null);
                        continue out;
                    }
                }

                filterSchema(entry::getValue, entry::setValue, schema, filters, context);
            }
        }

        filterSchema(
                schema::getAdditionalPropertiesSchema, schema::setAdditionalPropertiesSchema, schema, filters, context);

        List<Schema> allOf = schema.getAllOf();
        if (allOf != null) {
            ListIterator<Schema> it = allOf.listIterator();
            while (it.hasNext()) {
                filterSchema(it::next, it::set, schema, filters, context);
            }
        }

        List<Schema> oneOf = schema.getOneOf();
        if (oneOf != null) {
            ListIterator<Schema> it = oneOf.listIterator();
            while (it.hasNext()) {
                filterSchema(it::next, it::set, schema, filters, context);
            }
        }

        List<Schema> anyOf = schema.getAnyOf();
        if (anyOf != null) {
            ListIterator<Schema> it = anyOf.listIterator();
            while (it.hasNext()) {
                filterSchema(it::next, it::set, schema, filters, context);
            }
        }

        filterSchema(schema::getNot, schema::setNot, schema, filters, context);
    }

    private void filterSecuritySchemes(Components components, OpenAPIFilter[] filters, Context context) {
        if (components == null) {
            return;
        }

        Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();
        if (securitySchemes == null) {
            return;
        }

        Iterator<Entry<String, SecurityScheme>> it = securitySchemes.entrySet().iterator();
        out:
        while (it.hasNext()) {
            Entry<String, SecurityScheme> entry = it.next();
            SecurityScheme securityScheme = entry.getValue();
            SecurityScheme initialSecurityScheme = securityScheme;
            for (OpenAPIFilter filter : filters) {
                securityScheme = filter.filterSecurityScheme(securityScheme, context);
                if (securityScheme == null) {
                    it.remove();
                    continue out;
                }
            }
            if (securityScheme != initialSecurityScheme) {
                entry.setValue(securityScheme);
            }
        }
    }
}
