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

package org.apache.dubbo.xds.resource_new.filter;

import org.apache.dubbo.xds.resource_new.common.ConfigOrError;

import com.google.protobuf.Message;

/**
 * Defines the parsing functionality of an HTTP filter. A Filter may optionally implement either
 * {@link ClientFilter} or {@link ServerFilter} or both, indicating it is capable of working on
 * the client side or server side or both, respectively.
 */
public interface Filter {

    /**
     * The proto message types supported by this filter. A filter will be registered by each of its supported message
     * types.
     */
    String[] typeUrls();

    /**
     * Parses the top-level filter config from raw proto message. The message may be either a
     * {@link com.google.protobuf.Any} or a {@link com.google.protobuf.Struct}.
     */
    ConfigOrError<? extends FilterConfig> parseFilterConfig(Message rawProtoMessage);

    /**
     * Parses the per-filter override filter config from raw proto message. The message may be either a
     * {@link com.google.protobuf.Any} or a {@link com.google.protobuf.Struct}.
     */
    ConfigOrError<? extends FilterConfig> parseFilterConfigOverride(Message rawProtoMessage);


}
