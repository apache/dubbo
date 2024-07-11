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
package org.apache.dubbo.metadata.swagger.models.media;

import java.text.NumberFormat;
import java.util.Objects;

/**
 * IntegerSchema
 */
public class IntegerSchema extends Schema<Number> {

    public IntegerSchema() {
        super("integer", "int32");
    }

    @Override
    public IntegerSchema type(String type) {
        super.setType(type);
        return this;
    }

    @Override
    public IntegerSchema format(String format) {
        super.setFormat(format);
        return this;
    }

    public IntegerSchema _default(Number _default) {
        super.setDefault(_default);
        return this;
    }

    @Override
    protected Number cast(Object value) {
        if (value != null) {
            try {
                Number casted = NumberFormat.getInstance().parse(value.toString());
                if (Integer.MIN_VALUE <= casted.longValue() && casted.longValue() <= Integer.MAX_VALUE) {
                    return Integer.parseInt(value.toString());
                } else {
                    return Long.parseLong(value.toString());
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public IntegerSchema addEnumItem(Number _enumItem) {
        super.addEnumItemObject(_enumItem);
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
        sb.append("class IntegerSchema {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
