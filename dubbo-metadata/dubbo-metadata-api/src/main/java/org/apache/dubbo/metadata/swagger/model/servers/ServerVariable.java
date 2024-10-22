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
package org.apache.dubbo.metadata.swagger.model.servers;

import org.apache.dubbo.metadata.swagger.model.annotations.OpenAPI31;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ServerVariable
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.1/versions/3.0.1.md#serverVariableObject"
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.1.0/versions/3.1.0.md#serverVariableObject"
 */
public class ServerVariable {
    private List<String> _enum = null;
    private String _default = null;
    private String description = null;
    private java.util.Map<String, Object> extensions = null;

    /**
     * returns the _enum property from a ServerVariable instance.
     *
     * @return List&lt;String&gt; _enum
     **/
    public List<String> getEnum() {
        return _enum;
    }

    public void setEnum(List<String> _enum) {
        this._enum = _enum;
    }

    public ServerVariable _enum(List<String> _enum) {
        this._enum = _enum;
        return this;
    }

    public ServerVariable addEnumItem(String _enumItem) {
        if (this._enum == null) {
            this._enum = new ArrayList<>();
        }
        this._enum.add(_enumItem);
        return this;
    }

    /**
     * returns the _default property from a ServerVariable instance.
     *
     * @return String _default
     **/
    public String getDefault() {
        return _default;
    }

    public void setDefault(String _default) {
        this._default = _default;
    }

    public ServerVariable _default(String _default) {
        this._default = _default;
        return this;
    }

    /**
     * returns the description property from a ServerVariable instance.
     *
     * @return String description
     **/
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ServerVariable description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerVariable serverVariable = (ServerVariable) o;
        return Objects.equals(this._enum, serverVariable._enum)
                && Objects.equals(this._default, serverVariable._default)
                && Objects.equals(this.description, serverVariable.description)
                && Objects.equals(this.extensions, serverVariable.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_enum, _default, description, extensions);
    }

    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    public void addExtension(String name, Object value) {
        if (name == null || name.isEmpty() || !name.startsWith("x-")) {
            return;
        }
        if (this.extensions == null) {
            this.extensions = new java.util.LinkedHashMap<>();
        }
        this.extensions.put(name, value);
    }

    @OpenAPI31
    public void addExtension31(String name, Object value) {
        if (name != null && (name.startsWith("x-oas-") || name.startsWith("x-oai-"))) {
            return;
        }
        addExtension(name, value);
    }

    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    public ServerVariable extensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ServerVariable {\n");

        sb.append("    _enum: ").append(toIndentedString(_enum)).append("\n");
        sb.append("    _default: ").append(toIndentedString(_default)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
