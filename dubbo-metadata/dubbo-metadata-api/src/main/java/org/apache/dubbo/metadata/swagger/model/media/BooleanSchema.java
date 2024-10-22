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
package org.apache.dubbo.metadata.swagger.model.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * BooleanSchema
 */
public class BooleanSchema extends Schema<Boolean> {

    public BooleanSchema() {
        super("boolean", null);
    }

    @Override
    public BooleanSchema type(String type) {
        super.setType(type);
        return this;
    }

    @Override
    public BooleanSchema types(Set<String> types) {
        super.setTypes(types);
        return this;
    }

    public BooleanSchema _default(Boolean _default) {
        super.setDefault(_default);
        return this;
    }

    @Override
    protected Boolean cast(Object value) {
        if (value != null) {
            try {
                return Boolean.parseBoolean(value.toString());
            } catch (Exception e) {
            }
        }
        return null;
    }

    public BooleanSchema _enum(List<Boolean> _enum) {
        this._enum = _enum;
        return this;
    }

    public BooleanSchema addEnumItem(Boolean _enumItem) {
        if (this._enum == null) {
            this._enum = new ArrayList<Boolean>();
        }
        this._enum.add(_enumItem);
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
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BooleanSchema {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
