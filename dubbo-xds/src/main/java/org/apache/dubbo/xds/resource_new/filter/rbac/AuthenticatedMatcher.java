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
package org.apache.dubbo.xds.resource_new.filter.rbac;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource_new.matcher.StringMatcher;

final class AuthenticatedMatcher implements Matcher {

    @Nullable
    private final StringMatcher delegate;

    /**
     * Passing in null will match all authenticated user, i.e. SSL session is present.
     * https://github.com/envoyproxy/envoy/blob/3975bf5dadb43421907bbc52df57c0e8539c9a06/api/envoy/config/rbac/v3
     * /rbac.proto#L253
     */
    public static AuthenticatedMatcher create(@Nullable StringMatcher delegate) {
        return new AuthenticatedMatcher(delegate);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    AuthenticatedMatcher(@Nullable StringMatcher delegate) {
        this.delegate = delegate;
    }

    @Nullable
    public StringMatcher delegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "AuthenticatedMatcher{" + "delegate=" + delegate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof AuthenticatedMatcher) {
            AuthenticatedMatcher that = (AuthenticatedMatcher) o;
            return (this.delegate == null ? that.delegate() == null : this.delegate.equals(that.delegate()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (delegate == null) ? 0 : delegate.hashCode();
        return h$;
    }
}
