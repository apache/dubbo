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

public interface ServiceInfoV2OrBuilder
        extends
        // @@protoc_insertion_point(interface_extends:org.apache.dubbo.metadata.ServiceInfoV2)
        com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string name = 1;</code>
     * @return The name.
     */
    String getName();
    /**
     * <code>string name = 1;</code>
     * @return The bytes for name.
     */
    com.google.protobuf.ByteString getNameBytes();

    /**
     * <code>string group = 2;</code>
     * @return The group.
     */
    String getGroup();
    /**
     * <code>string group = 2;</code>
     * @return The bytes for group.
     */
    com.google.protobuf.ByteString getGroupBytes();

    /**
     * <code>string version = 3;</code>
     * @return The version.
     */
    String getVersion();
    /**
     * <code>string version = 3;</code>
     * @return The bytes for version.
     */
    com.google.protobuf.ByteString getVersionBytes();

    /**
     * <code>string protocol = 4;</code>
     * @return The protocol.
     */
    String getProtocol();
    /**
     * <code>string protocol = 4;</code>
     * @return The bytes for protocol.
     */
    com.google.protobuf.ByteString getProtocolBytes();

    /**
     * <code>int32 port = 5;</code>
     * @return The port.
     */
    int getPort();

    /**
     * <code>string path = 6;</code>
     * @return The path.
     */
    String getPath();
    /**
     * <code>string path = 6;</code>
     * @return The bytes for path.
     */
    com.google.protobuf.ByteString getPathBytes();

    /**
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    int getParamsCount();
    /**
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    boolean containsParams(String key);
    /**
     * Use {@link #getParamsMap()} instead.
     */
    @Deprecated
    java.util.Map<String, String> getParams();
    /**
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    java.util.Map<String, String> getParamsMap();
    /**
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    /* nullable */
    String getParamsOrDefault(
            String key,
            /* nullable */
            String defaultValue);
    /**
     * <code>map&lt;string, string&gt; params = 7;</code>
     */
    String getParamsOrThrow(String key);
}
