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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi.model;

import org.apache.dubbo.rpc.protocol.tri.rest.openapi.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SecurityRequirement extends Node<SecurityRequirement> {

    private Map<String, List<String>> requirements;

    public Map<String, List<String>> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<String, List<String>> requirements) {
        this.requirements = requirements;
    }

    public SecurityRequirement addRequirement(String name, String... scope) {
        return addRequirement(name, scope == null ? Collections.emptyList() : Arrays.asList(scope));
    }

    public SecurityRequirement addRequirement(String name, List<String> scopes) {
        if (requirements == null) {
            requirements = new LinkedHashMap<>();
        }
        if (scopes == null) {
            scopes = Collections.emptyList();
        }
        requirements.put(name, scopes);
        return this;
    }

    public void removeRequirement(String name) {
        if (requirements != null) {
            requirements.remove(name);
        }
    }

    @Override
    public SecurityRequirement clone() {
        SecurityRequirement clone = super.clone();
        if (requirements != null) {
            Map<String, List<String>> requirements = newMap(this.requirements.size());
            for (Map.Entry<String, List<String>> entry : this.requirements.entrySet()) {
                requirements.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            clone.requirements = requirements;
        }
        return clone;
    }

    @Override
    public Map<String, Object> writeTo(Map<String, Object> node, Context context) {
        write(node, "requirements", requirements);
        writeExtensions(node);
        return node;
    }
}
