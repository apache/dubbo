/*
 * Copyright 2022 The gRPC Authors
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

package org.apache.dubbo.xds;

import io.grpc.ChannelCredentials;
import io.grpc.Internal;

import java.util.Map;

/**
 * Provider of credentials which can be consumed by clients for xds communications. The actual
 * credential to be used for a particular xds communication will be chosen based on the bootstrap
 * configuration.
 *
 * <p>Implementations can be automatically discovered by gRPC via Java's SPI mechanism. For
 * automatic discovery, the implementation must have a zero-argument constructor and include
 * a resource named {@code META-INF/services/io.grpc.xds.XdsCredentialsProvider} in their JAR. The
 * file's contents should be the implementation's class name.
 * Implementations that need arguments in their constructor can be manually registered by
 * {@link XdsCredentialsRegistry#register}.
 *
 * <p>Implementations <em>should not</em> throw. If they do, it may interrupt class loading. If
 * exceptions may reasonably occur for implementation-specific reasons, implementations should
 * generally handle the exception gracefully and return {@code false} from {@link #isAvailable()}.
 */
@Internal
public abstract class XdsCredentialsProvider {
  /**
  * Creates a {@link ChannelCredentials} from the given jsonConfig, or
  * {@code null} if the given config is invalid. The provider is free to ignore
  * the config if it's not needed for producing the channel credentials.
  *
  * @param jsonConfig json config that can be consumed by the provider to create
  *                   the channel credentials
  *
  */
  public abstract ChannelCredentials newChannelCredentials(Map<String, ?> jsonConfig);

  /**
   * Returns the xDS credential name associated with this provider which makes it selectable
   * via {@link XdsCredentialsRegistry#getProvider}. This is called only when the class is loaded.
   * It shouldn't change, and there is no point doing so.
   */
  protected abstract String getName();

  /**
   * Whether this provider is available for use, taking the current environment
   * into consideration.
   * If {@code false}, {@link #newChannelCredentials} is not safe to be called.
   */
  public abstract boolean isAvailable();

  /**
   * A priority, from 0 to 10 that this provider should be used, taking the
   * current environment into consideration.
   * 5 should be considered the default, and then tweaked based on
   * environment detection. A priority of 0 does not imply that the provider
   * wouldn't work; just that it should be last in line.
   */
  public abstract int priority();
}
