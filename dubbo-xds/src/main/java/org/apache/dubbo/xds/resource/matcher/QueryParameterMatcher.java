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
package org.apache.dubbo.xds.resource.matcher;

import org.apache.dubbo.common.lang.Nullable;

import java.util.Objects;

public final class QueryParameterMatcher {

    private final String name;

    @Nullable
    private final StringMatcher stringMatcher;

    @Nullable
    private final Boolean present;

    public QueryParameterMatcher(String name, @Nullable StringMatcher stringMatcher, @Nullable Boolean present) {
        this.name = name;
        this.stringMatcher = stringMatcher;
        this.present = present;
    }

    public String getName() {
        return name;
    }

    public boolean matches(@Nullable String value) {
        if (present != null) {
            return present == (value != null);
        }
        if (stringMatcher != null) {
            return value != null && stringMatcher.matches(value);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryParameterMatcher that = (QueryParameterMatcher) o;
        return Objects.equals(name, that.name) && Objects.equals(
            stringMatcher, that.stringMatcher) && Objects.equals(present, that.present);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, stringMatcher, present);
    }

    @Override
    public String toString() {
        return "QueryParameterMatcher{" +
            "name='" + name + '\'' +
            ", stringMatcher=" + stringMatcher +
            ", present=" + present +
            '}';
    }
} 