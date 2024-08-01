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
import org.apache.dubbo.xds.resource_new.filter.FilterConfig;

final class RbacConfig implements FilterConfig {

    @Nullable
    private final AuthConfig authConfig;

    @Override
    public final String typeUrl() {
        return RbacFilter.TYPE_URL;
    }

    static RbacConfig create(@Nullable AuthConfig authConfig) {
        return new RbacConfig(authConfig);
    }

    RbacConfig(@Nullable AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    @Nullable
    AuthConfig authConfig() {
        return authConfig;
    }

    @Override
    public String toString() {
        return "RbacConfig{" + "authConfig=" + authConfig + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RbacConfig) {
            RbacConfig that = (RbacConfig) o;
            return (this.authConfig == null ? that.authConfig() == null : this.authConfig.equals(that.authConfig()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (authConfig == null) ? 0 : authConfig.hashCode();
        return h$;
    }
}
