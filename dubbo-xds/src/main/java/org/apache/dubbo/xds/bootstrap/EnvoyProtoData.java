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
package org.apache.dubbo.xds.bootstrap;

import javax.annotation.Nullable;

import java.util.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Defines gRPC data types for Envoy protobuf messages used in xDS protocol. Each data type has
 * the same name as Envoy's corresponding protobuf message, but only with fields used by gRPC.
 *
 * <p>Each data type should define a {@code fromEnvoyProtoXXX} static method to convert an Envoy
 * proto message to an instance of that data type.
 *
 * <p>For data types that need to be sent as protobuf messages, a {@code toEnvoyProtoXXX} instance
 * method is defined to convert an instance to Envoy proto message.
 *
 * <p>Data conversion should follow the invariant: converted data is guaranteed to be valid for
 * gRPC. If the protobuf message contains invalid data, the conversion should fail and no object
 * should be instantiated.
 */
@Internal
public final class EnvoyProtoData {

    // Prevent instantiation.
    private EnvoyProtoData() {}

    /**
     * See corresponding Envoy proto message {@link io.envoyproxy.envoy.config.core.v3.Node}.
     */
    public static final class Node {

        private final String id;
        private final String cluster;

        @Nullable
        private final Map<String, ?> metadata;

        @Nullable
        private final Locality locality;

        private final List<Address> listeningAddresses;
        private final String buildVersion;
        private final String userAgentName;

        @Nullable
        private final String userAgentVersion;

        private final List<String> clientFeatures;

        private Node(
                String id,
                String cluster,
                @Nullable Map<String, ?> metadata,
                @Nullable Locality locality,
                List<Address> listeningAddresses,
                String buildVersion,
                String userAgentName,
                @Nullable String userAgentVersion,
                List<String> clientFeatures) {
            this.id = checkNotNull(id, "id");
            this.cluster = checkNotNull(cluster, "cluster");
            this.metadata = metadata;
            this.locality = locality;
            this.listeningAddresses =
                    Collections.unmodifiableList(checkNotNull(listeningAddresses, "listeningAddresses"));
            this.buildVersion = checkNotNull(buildVersion, "buildVersion");
            this.userAgentName = checkNotNull(userAgentName, "userAgentName");
            this.userAgentVersion = userAgentVersion;
            this.clientFeatures = Collections.unmodifiableList(checkNotNull(clientFeatures, "clientFeatures"));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("cluster", cluster)
                    .add("metadata", metadata)
                    .add("locality", locality)
                    .add("listeningAddresses", listeningAddresses)
                    .add("buildVersion", buildVersion)
                    .add("userAgentName", userAgentName)
                    .add("userAgentVersion", userAgentVersion)
                    .add("clientFeatures", clientFeatures)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Node node = (Node) o;
            return Objects.equals(id, node.id)
                    && Objects.equals(cluster, node.cluster)
                    && Objects.equals(metadata, node.metadata)
                    && Objects.equals(locality, node.locality)
                    && Objects.equals(listeningAddresses, node.listeningAddresses)
                    && Objects.equals(buildVersion, node.buildVersion)
                    && Objects.equals(userAgentName, node.userAgentName)
                    && Objects.equals(userAgentVersion, node.userAgentVersion)
                    && Objects.equals(clientFeatures, node.clientFeatures);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    id,
                    cluster,
                    metadata,
                    locality,
                    listeningAddresses,
                    buildVersion,
                    userAgentName,
                    userAgentVersion,
                    clientFeatures);
        }

        public static final class Builder {
            private String id = "";
            private String cluster = "";

            @Nullable
            private Map<String, ?> metadata;

            @Nullable
            private Locality locality;
            // TODO(sanjaypujare): eliminate usage of listening_addresses field.
            private final List<Address> listeningAddresses = new ArrayList<>();
            private String buildVersion = "";
            private String userAgentName = "";

            @Nullable
            private String userAgentVersion;

            private final List<String> clientFeatures = new ArrayList<>();

            private Builder() {}

            @VisibleForTesting
            public Builder setId(String id) {
                this.id = checkNotNull(id, "id");
                return this;
            }

            @CanIgnoreReturnValue
            public Builder setCluster(String cluster) {
                this.cluster = checkNotNull(cluster, "cluster");
                return this;
            }

            @CanIgnoreReturnValue
            public Builder setMetadata(Map<String, ?> metadata) {
                this.metadata = checkNotNull(metadata, "metadata");
                return this;
            }

            @CanIgnoreReturnValue
            public Builder setLocality(Locality locality) {
                this.locality = checkNotNull(locality, "locality");
                return this;
            }

            @CanIgnoreReturnValue
            Builder addListeningAddresses(Address address) {
                listeningAddresses.add(checkNotNull(address, "address"));
                return this;
            }

            @CanIgnoreReturnValue
            public Builder setBuildVersion(String buildVersion) {
                this.buildVersion = checkNotNull(buildVersion, "buildVersion");
                return this;
            }

            @CanIgnoreReturnValue
            public Builder setUserAgentName(String userAgentName) {
                this.userAgentName = checkNotNull(userAgentName, "userAgentName");
                return this;
            }

            @CanIgnoreReturnValue
            public Builder setUserAgentVersion(String userAgentVersion) {
                this.userAgentVersion = checkNotNull(userAgentVersion, "userAgentVersion");
                return this;
            }

            @CanIgnoreReturnValue
            public Builder addClientFeatures(String clientFeature) {
                this.clientFeatures.add(checkNotNull(clientFeature, "clientFeature"));
                return this;
            }

            public Node build() {
                return new Node(
                        id,
                        cluster,
                        metadata,
                        locality,
                        listeningAddresses,
                        buildVersion,
                        userAgentName,
                        userAgentVersion,
                        clientFeatures);
            }
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder toBuilder() {
            Builder builder = new Builder();
            builder.id = id;
            builder.cluster = cluster;
            builder.metadata = metadata;
            builder.locality = locality;
            builder.buildVersion = buildVersion;
            builder.listeningAddresses.addAll(listeningAddresses);
            builder.userAgentName = userAgentName;
            builder.userAgentVersion = userAgentVersion;
            builder.clientFeatures.addAll(clientFeatures);
            return builder;
        }

        public String getId() {
            return id;
        }

        String getCluster() {
            return cluster;
        }

        @Nullable
        Map<String, ?> getMetadata() {
            return metadata;
        }

        @Nullable
        Locality getLocality() {
            return locality;
        }

        List<Address> getListeningAddresses() {
            return listeningAddresses;
        }
    }

    /**
     * Converts Java representation of the given JSON value to protobuf's {@link
     * Value} representation.
     *
     * <p>The given {@code rawObject} must be a valid JSON value in Java representation, which is
     * either a {@code Map<String, ?>}, {@code List<?>}, {@code String}, {@code Double}, {@code
     * Boolean}, or {@code null}.
     */
    private static Value convertToValue(Object rawObject) {
        Value.Builder valueBuilder = Value.newBuilder();
        if (rawObject == null) {
            valueBuilder.setNullValue(NullValue.NULL_VALUE);
        } else if (rawObject instanceof Double) {
            valueBuilder.setNumberValue((Double) rawObject);
        } else if (rawObject instanceof String) {
            valueBuilder.setStringValue((String) rawObject);
        } else if (rawObject instanceof Boolean) {
            valueBuilder.setBoolValue((Boolean) rawObject);
        } else if (rawObject instanceof Map) {
            Struct.Builder structBuilder = Struct.newBuilder();
            @SuppressWarnings("unchecked")
            Map<String, ?> map = (Map<String, ?>) rawObject;
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                structBuilder.putFields(entry.getKey(), convertToValue(entry.getValue()));
            }
            valueBuilder.setStructValue(structBuilder);
        } else if (rawObject instanceof List) {
            ListValue.Builder listBuilder = ListValue.newBuilder();
            List<?> list = (List<?>) rawObject;
            for (Object obj : list) {
                listBuilder.addValues(convertToValue(obj));
            }
            valueBuilder.setListValue(listBuilder);
        }
        return valueBuilder.build();
    }

    /**
     * See corresponding Envoy proto message {@link io.envoyproxy.envoy.config.core.v3.Address}.
     */
    static final class Address {
        private final String address;
        private final int port;

        Address(String address, int port) {
            this.address = checkNotNull(address, "address");
            this.port = port;
        }

        io.envoyproxy.envoy.config.core.v3.Address toEnvoyProtoAddress() {
            return io.envoyproxy.envoy.config.core.v3.Address.newBuilder()
                    .setSocketAddress(io.envoyproxy.envoy.config.core.v3.SocketAddress.newBuilder()
                            .setAddress(address)
                            .setPortValue(port))
                    .build();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("address", address)
                    .add("port", port)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Address address1 = (Address) o;
            return port == address1.port && Objects.equals(address, address1.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, port);
        }
    }
}
