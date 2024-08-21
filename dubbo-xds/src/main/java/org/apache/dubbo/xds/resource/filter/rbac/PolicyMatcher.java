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
package org.apache.dubbo.xds.resource.filter.rbac;

final class PolicyMatcher implements Matcher {

    private final String name;

    private final OrMatcher permissions;

    private final OrMatcher principals;

    /**
     * Constructs a matcher for one RBAC policy.
     */
    public static PolicyMatcher create(String name, OrMatcher permissions, OrMatcher principals) {
        return new PolicyMatcher(name, permissions, principals);
    }

    @Override
    public boolean matches(Object args) {
        return getPermissions().matches(args) && getPrincipals().matches(args);
    }

    PolicyMatcher(String name, OrMatcher permissions, OrMatcher principals) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        if (permissions == null) {
            throw new NullPointerException("Null permissions");
        }
        this.permissions = permissions;
        if (principals == null) {
            throw new NullPointerException("Null principals");
        }
        this.principals = principals;
    }

    public String getName() {
        return name;
    }

    public OrMatcher getPermissions() {
        return permissions;
    }

    public OrMatcher getPrincipals() {
        return principals;
    }

    @Override
    public String toString() {
        return "PolicyMatcher{" + "name=" + name + ", " + "permissions=" + permissions + ", " + "principals="
                + principals + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PolicyMatcher) {
            PolicyMatcher that = (PolicyMatcher) o;
            return this.name.equals(that.getName())
                    && this.permissions.equals(that.getPermissions())
                    && this.principals.equals(that.getPrincipals());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= name.hashCode();
        h$ *= 1000003;
        h$ ^= permissions.hashCode();
        h$ *= 1000003;
        h$ ^= principals.hashCode();
        return h$;
    }
}
