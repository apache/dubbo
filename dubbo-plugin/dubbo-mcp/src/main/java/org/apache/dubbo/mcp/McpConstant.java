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
package org.apache.dubbo.mcp;

/**
 * Constants for MCP (Model Context Protocol) integration
 */
public interface McpConstant {

    String SETTINGS_MCP_PREFIX = "dubbo.protocol.triple.rest.mcp";
    String SETTINGS_MCP_ENABLE = "dubbo.protocol.triple.rest.mcp.enabled";
    String SETTINGS_MCP_PORT = "dubbo.protocol.triple.rest.mcp.port";
    String SETTINGS_MCP_PROTOCOL = "dubbo.protocol.triple.rest.mcp.protocol";
    String SETTINGS_MCP_SESSION_TIMEOUT = "dubbo.protocol.triple.rest.mcp.session-timeout";
    String SETTINGS_MCP_DEFAULT_ENABLED = "dubbo.protocol.triple.rest.mcp.default.enabled";
    String SETTINGS_MCP_INCLUDE_PATTERNS = "dubbo.protocol.triple.rest.mcp.include-patterns";
    String SETTINGS_MCP_EXCLUDE_PATTERNS = "dubbo.protocol.triple.rest.mcp.exclude-patterns";
    String SETTINGS_MCP_PATHS_SSE = "dubbo.protocol.triple.rest.mcp.path.sse";
    String SETTINGS_MCP_PATHS_MESSAGE = "dubbo.protocol.triple.rest.mcp.path.message";

    Integer DEFAULT_SESSION_TIMEOUT = 60;

    // MCP service control-related configuration
    String SETTINGS_MCP_SERVICE_PREFIX = "dubbo.protocol.triple.rest.mcp.service";
    String SETTINGS_MCP_SERVICE_ENABLED_SUFFIX = "enabled";
    String SETTINGS_MCP_SERVICE_NAME_SUFFIX = "tool-name";
    String SETTINGS_MCP_SERVICE_DESCRIPTION_SUFFIX = "description";
    String SETTINGS_MCP_SERVICE_TAGS_SUFFIX = "tags";

    String MCP_SERVICE_PROTOCOL = "tri";
    int MCP_SERVICE_PORT = 8081;

    String DEFAULT_TOOL_NAME_PREFIX = "arg";
    String DEFAULT_TOOL_DESCRIPTION_TEMPLATE = "Execute method '%s' from service '%s'";
    String DEFAULT_PARAMETER_DESCRIPTION_TEMPLATE = "Parameter %d of type %s";

    String SCHEMA_PROPERTY_TYPE = "type";
    String SCHEMA_PROPERTY_DESCRIPTION = "description";
    String SCHEMA_PROPERTY_PROPERTIES = "properties";

    // Common JSON Schema property names
    String SCHEMA_PROPERTY_FORMAT = "format";
    String SCHEMA_PROPERTY_ENUM = "enum";
    String SCHEMA_PROPERTY_DEFAULT = "default";
    String SCHEMA_PROPERTY_REF = "$ref";
    String SCHEMA_PROPERTY_ITEMS = "items";
    String SCHEMA_PROPERTY_ADDITIONAL_PROPERTIES = "additionalProperties";
    String SCHEMA_PROPERTY_REQUIRED = "required";

    // Specific parameter names
    String PARAM_TRIPLE_SERVICE_GROUP = "tri-service-group";
    String PARAM_REQUEST_BODY_PAYLOAD = "requestBodyPayload";

    // URL parameter names for MCP configuration
    String PARAM_MCP_ENABLED = "mcp.enabled";
    String PARAM_MCP_TOOL_NAME = "mcp.tool-name";
    String PARAM_MCP_DESCRIPTION = "mcp.description";
    String PARAM_MCP_TAGS = "mcp.tags";
    String PARAM_MCP_PRIORITY = "mcp.priority";

    // Default parameter descriptions
    String PARAM_DESCRIPTION_DOUBLE = "A numeric value of type Double";
    String PARAM_DESCRIPTION_INTEGER = "An integer value";
    String PARAM_DESCRIPTION_STRING = "A string value";
    String PARAM_DESCRIPTION_BOOLEAN = "A boolean value";
}
