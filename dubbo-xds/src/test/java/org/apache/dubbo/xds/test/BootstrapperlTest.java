/*
 * Copyright 2019 The gRPC Authors
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

package org.apache.dubbo.xds.test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.grpc.InsecureChannelCredentials;
import io.grpc.TlsChannelCredentials;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.GrpcUtil.GrpcBuildVersion;

import org.apache.dubbo.xds.XdsInitializationException;
import org.apache.dubbo.xds.bootstrap.Bootstrapper;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.AuthorityInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.BootstrapInfo;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.FileReader;
import org.apache.dubbo.xds.bootstrap.Bootstrapper.ServerInfo;

import org.apache.dubbo.xds.bootstrap.EnvoyProtoData.Node;
import org.apache.dubbo.xds.bootstrap.Locality;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/** Unit tests for {@link Bootstrapper}. */
@RunWith(JUnit4.class)
public class BootstrapperlTest {

  private static final String BOOTSTRAP_FILE_PATH = "C:\\Users\\Windows 10\\Desktop\\grpc-bootstrap.json";
  private static final String SERVER_URI = "unix:///etc/istio/proxy/XDS";
  @SuppressWarnings("deprecation") // https://github.com/grpc/grpc-java/issues/7467
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private final Bootstrapper bootstrapper = new Bootstrapper();
  private String originalBootstrapPathFromEnvVar;
  private String originalBootstrapPathFromSysProp;
  private String originalBootstrapConfigFromEnvVar;
  private String originalBootstrapConfigFromSysProp;

  @Before
  public void setUp() {
    saveEnvironment();
    bootstrapper.bootstrapPathFromEnvVar = BOOTSTRAP_FILE_PATH;
  }

  private void saveEnvironment() {
    originalBootstrapPathFromEnvVar = bootstrapper.bootstrapPathFromEnvVar;
    originalBootstrapConfigFromEnvVar = bootstrapper.bootstrapConfigFromEnvVar;
  }

  @After
  public void restoreEnvironment() {
    bootstrapper.bootstrapPathFromEnvVar = originalBootstrapPathFromEnvVar;
    bootstrapper.bootstrapConfigFromEnvVar = originalBootstrapConfigFromEnvVar;
  }

  @Test
  public void parseBootstrap_singleXdsServer() throws XdsInitializationException {
    BootstrapInfo info = bootstrapper.bootstrap();
  }

}
