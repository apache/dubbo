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
package org.apache.dubbo.metadata.swagger.model;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class OperationService {

    public String getOperationId(String operationId, String oldOperationId, OpenAPI openAPI) {
        if (StringUtils.isNotBlank(oldOperationId)) return this.getOperationId(oldOperationId, openAPI);
        else return this.getOperationId(operationId, openAPI);
    }

    public String getOperationId(String operationId, OpenAPI openAPI) {
        boolean operationIdUsed = this.existOperationId(operationId, openAPI);
        String operationIdToFind = null;
        int counter = 0;
        while (operationIdUsed) {
            operationIdToFind = String.format("%s_%d", operationId, ++counter);
            operationIdUsed = this.existOperationId(operationIdToFind, openAPI);
        }
        if (operationIdToFind != null) {
            operationId = operationIdToFind;
        }
        return operationId;
    }

    private boolean existOperationId(String operationId, OpenAPI openAPI) {
        if (openAPI == null) {
            return false;
        }
        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            return false;
        }
        for (PathItem path : openAPI.getPaths().values()) {
            Set<String> pathOperationIds = this.extractOperationIdFromPathItem(path);
            if (pathOperationIds.contains(operationId)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractOperationIdFromPathItem(PathItem path) {
        Set<String> ids = new HashSet<>();
        if (path.getGet() != null && StringUtils.isNotBlank(path.getGet().getOperationId())) {
            ids.add(path.getGet().getOperationId());
        }
        if (path.getPost() != null && StringUtils.isNotBlank(path.getPost().getOperationId())) {
            ids.add(path.getPost().getOperationId());
        }
        if (path.getPut() != null && StringUtils.isNotBlank(path.getPut().getOperationId())) {
            ids.add(path.getPut().getOperationId());
        }
        if (path.getDelete() != null && StringUtils.isNotBlank(path.getDelete().getOperationId())) {
            ids.add(path.getDelete().getOperationId());
        }
        if (path.getOptions() != null
                && StringUtils.isNotBlank(path.getOptions().getOperationId())) {
            ids.add(path.getOptions().getOperationId());
        }
        if (path.getHead() != null && StringUtils.isNotBlank(path.getHead().getOperationId())) {
            ids.add(path.getHead().getOperationId());
        }
        if (path.getPatch() != null && StringUtils.isNotBlank(path.getPatch().getOperationId())) {
            ids.add(path.getPatch().getOperationId());
        }
        return ids;
    }
}
