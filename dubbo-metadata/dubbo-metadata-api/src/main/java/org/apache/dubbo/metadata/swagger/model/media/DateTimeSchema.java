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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * DateTimeSchema
 */
public class DateTimeSchema extends Schema<OffsetDateTime> {

    public DateTimeSchema() {
        super("string", "date-time");
    }

    @Override
    public DateTimeSchema type(String type) {
        super.setType(type);
        return this;
    }

    @Override
    public DateTimeSchema format(String format) {
        super.setFormat(format);
        return this;
    }

    public DateTimeSchema _default(Date _default) {
        super.setDefault(_default);
        return this;
    }

    @Override
    protected OffsetDateTime cast(Object value) {
        if (value != null) {
            try {
                if (value instanceof Date) {
                    return ((Date) value).toInstant().atOffset(ZoneOffset.UTC);
                } else if (value instanceof String) {
                    return OffsetDateTime.parse((String) value);
                } else if (value instanceof OffsetDateTime) {
                    return (OffsetDateTime) value;
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public DateTimeSchema _enum(List<OffsetDateTime> _enum) {
        super.setEnum(_enum);
        return this;
    }

    public DateTimeSchema addEnumItem(OffsetDateTime _enumItem) {
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
        sb.append("class DateTimeSchema {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
