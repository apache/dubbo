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
package org.apache.dubbo.xds.resource.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.matcher.StringMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TLS context match options for matching TLS certificates.
 */
public final class TlsContextMatchOptions {

    @Nullable
    private final Boolean presented;

    @Nullable
    private final Boolean validated;

    @Nullable
    private final List<StringMatcher> sanMatchers;

    public TlsContextMatchOptions(
            @Nullable Boolean presented, @Nullable Boolean validated, @Nullable List<StringMatcher> sanMatchers) {
        this.presented = presented;
        this.validated = validated;
        this.sanMatchers = sanMatchers != null ? Collections.unmodifiableList(new ArrayList<>(sanMatchers)) : null;
    }

    @Nullable
    public Boolean getPresented() {
        return presented;
    }

    @Nullable
    public Boolean getValidated() {
        return validated;
    }

    @Nullable
    public List<StringMatcher> getSanMatchers() {
        return sanMatchers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TlsContextMatchOptions that = (TlsContextMatchOptions) o;
        return Objects.equals(presented, that.presented)
                && Objects.equals(validated, that.validated)
                && Objects.equals(sanMatchers, that.sanMatchers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(presented, validated, sanMatchers);
    }

    @Override
    public String toString() {
        return "TlsContextMatchOptions{" + "presented="
                + presented + ", validated="
                + validated + ", sanMatchers="
                + sanMatchers + '}';
    }
}
