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

import java.util.Map;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;

/**
 * A wrapper class that supports {@link InsecureChannelCredentials} for Xds
 * by implementing {@link XdsCredentialsProvider}.
 */
public final class InsecureXdsCredentialsProvider extends XdsCredentialsProvider {
  private static final String CREDS_NAME = "insecure";

  @Override
  public ChannelCredentials newChannelCredentials(Map<String, ?> jsonConfig) {
    return InsecureChannelCredentials.create();
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
