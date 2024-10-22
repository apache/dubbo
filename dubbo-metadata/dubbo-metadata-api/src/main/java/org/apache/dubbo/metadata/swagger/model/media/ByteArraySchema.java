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

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * ByteArraySchema
 */
public class ByteArraySchema extends Schema<byte[]> {

    public ByteArraySchema() {
        super("string", "byte");
    }

    @Override
    public ByteArraySchema type(String type) {
        super.setType(type);
        return this;
    }

    @Override
    public ByteArraySchema format(String format) {
        super.setFormat(format);
        return this;
    }

    public ByteArraySchema _default(byte[] _default) {
        super.setDefault(_default);
        return this;
    }

    @Override
    protected byte[] cast(Object value) {
        if (value != null) {
            try {
                if (value instanceof byte[]) {
                    return (byte[]) value;
                } else if (value instanceof String) {
                    if ((System.getProperty(BINARY_STRING_CONVERSION_PROPERTY) != null
                                    && System.getProperty(BINARY_STRING_CONVERSION_PROPERTY)
                                            .equals(BynaryStringConversion.BINARY_STRING_CONVERSION_BASE64.toString()))
                            || (System.getenv(BINARY_STRING_CONVERSION_PROPERTY) != null
                                    && System.getenv(BINARY_STRING_CONVERSION_PROPERTY)
                                            .equals(
                                                    BynaryStringConversion.BINARY_STRING_CONVERSION_BASE64
                                                            .toString()))) {
                        return Base64.getDecoder().decode((String) value);
                    }
                    return value.toString().getBytes();
                } else {
                    return value.toString().getBytes();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public ByteArraySchema _enum(List<byte[]> _enum) {
        super.setEnum(_enum);
        return this;
    }

    public ByteArraySchema addEnumItem(byte[] _enumItem) {
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
        sb.append("class ByteArraySchema {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
