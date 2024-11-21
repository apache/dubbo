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

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;

public interface OpenAPINamingStrategy extends OpenAPIExtension {

    String generateOperationId(MethodMeta methodMeta, OpenAPI openAPI);

    String resolveOperationIdConflict(int attempt, String operationId, MethodMeta methodMeta, OpenAPI openAPI);

    String generateSchemaName(Class<?> clazz, OpenAPI openAPI);

    String resolveSchemaNameConflict(int attempt, String schemaName, Class<?> clazz, OpenAPI openAPI);
}
