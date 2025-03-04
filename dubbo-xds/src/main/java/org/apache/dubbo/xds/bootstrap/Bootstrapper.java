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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Bootstrapper {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrapper.class);
    private static final String BOOTSTRAP_PATH_SYS_ENV_VAR = "GRPC_XDS_BOOTSTRAP";
    private static final String DEFAULT_BOOTSTRAP_PATH = "/bootstrap.json";
    private final BootstrapInfo bootstrapInfo;

    private static final class InstanceHolder {
        static final Bootstrapper INSTANCE = new Bootstrapper();
    }

    public static Bootstrapper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public Bootstrapper() {
        String jsonContent = getJsonContent();
        this.bootstrapInfo = JsonUtils.getJson().toJavaObject(jsonContent, BootstrapInfo.class);
    }

    private String getJsonContent() {
        String jsonContent;
        String filePath = null;

        // Get the path of the bootstrap config via environment variable and system property
        String bootstrapPathFromEnvVar = System.getenv(BOOTSTRAP_PATH_SYS_ENV_VAR);
        if (bootstrapPathFromEnvVar == null) {
            bootstrapPathFromEnvVar = System.getProperty(BOOTSTRAP_PATH_SYS_ENV_VAR);
        }

        // Check environment variable and system property
        if (bootstrapPathFromEnvVar != null && Files.exists(Paths.get(bootstrapPathFromEnvVar))) {
            filePath = bootstrapPathFromEnvVar;
        } else if (Files.exists(Paths.get(DEFAULT_BOOTSTRAP_PATH))) {
            // Check the default path
            filePath = DEFAULT_BOOTSTRAP_PATH;
        }
        if (filePath != null) {
            logger.info("Reading bootstrap file from {0}", filePath);
            try {
                jsonContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("Reading bootstrap from " + filePath);
        } else {
            jsonContent = null;
        }

        return jsonContent;
    }

    public BootstrapInfo bootstrap() {
        return bootstrapInfo;
    }
}
