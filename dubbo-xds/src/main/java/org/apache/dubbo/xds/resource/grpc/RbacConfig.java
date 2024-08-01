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

import org.apache.dubbo.xds.resource.grpc.Filter.FilterConfig;
import org.apache.dubbo.xds.resource.grpc.GrpcAuthorizationEngine.AuthConfig;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/** Rbac configuration for Rbac filter. */
@AutoValue
abstract class RbacConfig implements FilterConfig {
  @Override
  public final String typeUrl() {
    return RbacFilter.TYPE_URL;
  }

  @Nullable
  abstract AuthConfig authConfig();

  static RbacConfig create(@Nullable AuthConfig authConfig) {
    return new AutoValue_RbacConfig(authConfig);
  }
}
