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

public interface OpenAPIRequestOrBuilder
        extends
        // @@protoc_insertion_point(interface_extends:org.apache.dubbo.metadata.OpenAPIRequest)
        com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * The openAPI group.
     * </pre>
     *
     * <code>string group = 1;</code>
     * @return The group.
     */
    String getGroup();

    /**
     * <pre>
     * The openAPI group.
     * </pre>
     *
     * <code>string group = 1;</code>
     * @return The bytes for group.
     */
    com.google.protobuf.ByteString getGroupBytes();

    /**
     * <pre>
     * The openAPI version, using a major.minor.patch versioning scheme
     * e.g. 1.0.1
     * </pre>
     *
     * <code>string version = 2;</code>
     * @return The version.
     */
    String getVersion();

    /**
     * <pre>
     * The openAPI version, using a major.minor.patch versioning scheme
     * e.g. 1.0.1
     * </pre>
     *
     * <code>string version = 2;</code>
     * @return The bytes for version.
     */
    com.google.protobuf.ByteString getVersionBytes();

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @return A list containing the tag.
     */
    java.util.List<String> getTagList();

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @return The count of tag.
     */
    int getTagCount();

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @param index The index of the element to return.
     * @return The tag at the given index.
     */
    String getTag(int index);

    /**
     * <pre>
     * The openAPI tags. Each tag is an or condition.
     * </pre>
     *
     * <code>repeated string tag = 3;</code>
     * @param index The index of the value to return.
     * @return The bytes of the tag at the given index.
     */
    com.google.protobuf.ByteString getTagBytes(int index);

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @return A list containing the service.
     */
    java.util.List<String> getServiceList();

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @return The count of service.
     */
    int getServiceCount();

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @param index The index of the element to return.
     * @return The service at the given index.
     */
    String getService(int index);

    /**
     * <pre>
     * The openAPI services. Each service is an or condition.
     * </pre>
     *
     * <code>repeated string service = 4;</code>
     * @param index The index of the value to return.
     * @return The bytes of the service at the given index.
     */
    com.google.protobuf.ByteString getServiceBytes(int index);

    /**
     * <pre>
     * The openAPI specification version, using a major.minor.patch versioning scheme
     * e.g. 3.0.1, 3.1.0
     * The default value is '3.0.1'.
     * </pre>
     *
     * <code>string openapi = 5;</code>
     * @return The openapi.
     */
    String getOpenapi();

    /**
     * <pre>
     * The openAPI specification version, using a major.minor.patch versioning scheme
     * e.g. 3.0.1, 3.1.0
     * The default value is '3.0.1'.
     * </pre>
     *
     * <code>string openapi = 5;</code>
     * @return The bytes for openapi.
     */
    com.google.protobuf.ByteString getOpenapiBytes();

    /**
     * <pre>
     * The format of the response.
     * The default value is 'JSON'.
     * </pre>
     *
     * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
     * @return Whether the format field is set.
     */
    boolean hasFormat();

    /**
     * <pre>
     * The format of the response.
     * The default value is 'JSON'.
     * </pre>
     *
     * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
     * @return The enum numeric value on the wire for format.
     */
    int getFormatValue();

    /**
     * <pre>
     * The format of the response.
     * The default value is 'JSON'.
     * </pre>
     *
     * <code>optional .org.apache.dubbo.metadata.OpenAPIFormat format = 6;</code>
     * @return The format.
     */
    OpenAPIFormat getFormat();

    /**
     * <pre>
     * Whether to pretty print for json.
     * The default value is 'false'.
     * </pre>
     *
     * <code>optional bool pretty = 7;</code>
     * @return Whether the pretty field is set.
     */
    boolean hasPretty();

    /**
     * <pre>
     * Whether to pretty print for json.
     * The default value is 'false'.
     * </pre>
     *
     * <code>optional bool pretty = 7;</code>
     * @return The pretty.
     */
    boolean getPretty();
}
