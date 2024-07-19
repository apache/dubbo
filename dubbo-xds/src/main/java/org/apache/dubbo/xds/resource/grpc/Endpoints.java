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

package org.apache.dubbo.xds.resource.grpc;

import java.net.InetSocketAddress;
import java.util.List;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.grpc.EquivalentAddressGroup;

import static com.google.common.base.Preconditions.checkArgument;

/** Locality and endpoint level load balancing configurations. */
final class Endpoints {
  private Endpoints() {}

  /** Represents a group of endpoints belong to a single locality. */
  @AutoValue
  abstract static class LocalityLbEndpoints {
    // Endpoints to be load balanced.
    abstract ImmutableList<LbEndpoint> endpoints();

    // Locality's weight for inter-locality load balancing. Guaranteed to be greater than 0.
    abstract int localityWeight();

    // Locality's priority level.
    abstract int priority();

    static LocalityLbEndpoints create(List<LbEndpoint> endpoints, int localityWeight,
        int priority) {
      checkArgument(localityWeight > 0, "localityWeight must be greater than 0");
      return new AutoValue_Endpoints_LocalityLbEndpoints(
          ImmutableList.copyOf(endpoints), localityWeight, priority);
    }
  }

  /** Represents a single endpoint to be load balanced. */
  @AutoValue
  abstract static class LbEndpoint {
    // The endpoint address to be connected to.
    abstract EquivalentAddressGroup eag();

    // Endpoint's weight for load balancing. If unspecified, value of 0 is returned.
    abstract int loadBalancingWeight();

    // Whether the endpoint is healthy.
    abstract boolean isHealthy();

    static LbEndpoint create(EquivalentAddressGroup eag, int loadBalancingWeight,
        boolean isHealthy) {
      return new AutoValue_Endpoints_LbEndpoint(eag, loadBalancingWeight, isHealthy);
    }

    // Only for testing.
    @VisibleForTesting
    static LbEndpoint create(
        String address, int port, int loadBalancingWeight, boolean isHealthy) {
      return LbEndpoint.create(new EquivalentAddressGroup(new InetSocketAddress(address, port)),
          loadBalancingWeight, isHealthy);
    }
  }

  /** Represents a drop policy. */
  @AutoValue
  abstract static class DropOverload {
    abstract String category();

    abstract int dropsPerMillion();

    static DropOverload create(String category, int dropsPerMillion) {
      return new AutoValue_Endpoints_DropOverload(category, dropsPerMillion);
    }
  }
}
