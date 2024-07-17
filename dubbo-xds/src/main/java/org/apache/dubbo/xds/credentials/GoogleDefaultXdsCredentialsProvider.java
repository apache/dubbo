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

package org.apache.dubbo.xds.credentials;

import io.grpc.ChannelCredentials;
import io.grpc.alts.GoogleDefaultChannelCredentials;

import java.util.Map;

/**
 * A wrapper class that supports {@link GoogleDefaultChannelCredentials} for
 * Xds by implementing {@link XdsCredentialsProvider}.
 */
public final class GoogleDefaultXdsCredentialsProvider extends XdsCredentialsProvider {
  private static final String CREDS_NAME = "google_default";

  @Override
  public ChannelCredentials newChannelCredentials(Map<String, ?> jsonConfig) {
    return GoogleDefaultChannelCredentials.create();
  }

  @Override
  protected String getName() {
    return CREDS_NAME;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public int priority() {
    return 5;
  }
}
