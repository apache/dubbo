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
package org.apache.dubbo.xds.resource.filter;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.filter.fault.FaultFilter;
import org.apache.dubbo.xds.resource.filter.rbac.RbacFilter;
import org.apache.dubbo.xds.resource.filter.router.RouterFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for all supported {@link Filter}s. Filters can be queried from the registry by any of the
 * {@link Filter#typeUrls() type URLs}.
 */
public class FilterRegistry {
    private static FilterRegistry instance;

    private final Map<String, Filter> supportedFilters = new HashMap<>();

    private FilterRegistry() {}

    public static synchronized FilterRegistry getDefaultRegistry() {
        if (instance == null) {
            instance = newRegistry().register(FaultFilter.INSTANCE, RouterFilter.INSTANCE, RbacFilter.INSTANCE);
        }
        return instance;
    }

    static FilterRegistry newRegistry() {
        return new FilterRegistry();
    }

    FilterRegistry register(Filter... filters) {
        for (Filter filter : filters) {
            for (String typeUrl : filter.typeUrls()) {
                supportedFilters.put(typeUrl, filter);
            }
        }
        return this;
    }

    @Nullable
    public Filter get(String typeUrl) {
        return supportedFilters.get(typeUrl);
    }
}
