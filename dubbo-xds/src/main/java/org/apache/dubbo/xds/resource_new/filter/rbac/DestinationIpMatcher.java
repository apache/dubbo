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

import org.apache.dubbo.xds.resource_new.matcher.CidrMatcher;

final class DestinationIpMatcher implements Matcher {

    private final CidrMatcher delegate;

    public static DestinationIpMatcher create(CidrMatcher delegate) {
        return new DestinationIpMatcher(delegate);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    DestinationIpMatcher(CidrMatcher delegate) {
        if (delegate == null) {
            throw new NullPointerException("Null delegate");
        }
        this.delegate = delegate;
    }

    public CidrMatcher delegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return "DestinationIpMatcher{" + "delegate=" + delegate + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DestinationIpMatcher) {
            DestinationIpMatcher that = (DestinationIpMatcher) o;
            return this.delegate.equals(that.delegate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= delegate.hashCode();
        return h$;
    }
}
