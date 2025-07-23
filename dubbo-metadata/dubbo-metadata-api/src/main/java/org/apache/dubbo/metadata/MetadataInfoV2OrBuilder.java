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
package org.apache.dubbo.metadata;

public interface MetadataInfoV2OrBuilder
        extends
        // @@protoc_insertion_point(interface_extends:org.apache.dubbo.metadata.MetadataInfoV2)
        com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * The application name.
     * </pre>
     *
     * <code>string app = 1;</code>
     * @return The app.
     */
    String getApp();

    /**
     * <pre>
     * The application name.
     * </pre>
     *
     * <code>string app = 1;</code>
     * @return The bytes for app.
     */
    com.google.protobuf.ByteString getAppBytes();

    /**
     * <pre>
     * The application version.
     * </pre>
     *
     * <code>string version = 2;</code>
     * @return The version.
     */
    String getVersion();

    /**
     * <pre>
     * The application version.
     * </pre>
     *
     * <code>string version = 2;</code>
     * @return The bytes for version.
     */
    com.google.protobuf.ByteString getVersionBytes();

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    int getServicesCount();

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    boolean containsServices(String key);

    /**
     * Use {@link #getServicesMap()} instead.
     */
    @Deprecated
    java.util.Map<String, ServiceInfoV2> getServices();

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    java.util.Map<String, ServiceInfoV2> getServicesMap();

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    /* nullable */
    ServiceInfoV2 getServicesOrDefault(
            String key,
            /* nullable */
            ServiceInfoV2 defaultValue);

    /**
     * <pre>
     * A map of service information.
     * </pre>
     *
     * <code>map&lt;string, .org.apache.dubbo.metadata.ServiceInfoV2&gt; services = 3;</code>
     */
    ServiceInfoV2 getServicesOrThrow(String key);
}
