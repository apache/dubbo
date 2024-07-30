/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource_new.filter.router;

import org.apache.dubbo.xds.resource_new.common.ConfigOrError;
import org.apache.dubbo.xds.resource_new.filter.Filter;
import org.apache.dubbo.xds.resource_new.filter.FilterConfig;

import com.google.protobuf.Message;

/**
 * Router filter implementation. Currently this filter does not parse any field in the config.
 */
public enum RouterFilter implements Filter {
    INSTANCE;

    static final String TYPE_URL = "type.googleapis.com/envoy.extensions.filters.http.router.v3.Router";

    public static final FilterConfig ROUTER_CONFIG = new FilterConfig() {

        public String typeUrl() {
            return RouterFilter.TYPE_URL;
        }

        public String toString() {
            return "ROUTER_CONFIG";
        }
    };

    public String[] typeUrls() {
        return new String[] {TYPE_URL};
    }

    public ConfigOrError<? extends FilterConfig> parseFilterConfig(Message rawProtoMessage) {
        return ConfigOrError.fromConfig(ROUTER_CONFIG);
    }

    public ConfigOrError<? extends FilterConfig> parseFilterConfigOverride(Message rawProtoMessage) {
        return ConfigOrError.fromError("Router Filter should not have override config");
    }
}
