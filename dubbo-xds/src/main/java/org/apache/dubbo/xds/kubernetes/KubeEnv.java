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
package org.apache.dubbo.xds.kubernetes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KubeEnv {

    private KubeEnv() {
        // 之后考虑通过config读取...
    }

    public String getApiServerPath() {
        // ... get from env...
        return "https://kubernetes.default.svc";
    }

    public boolean enableSsl() {
        return false;
    }

    public String getServiceAccountPath() {
        return "/var/run/secrets/kubernetes.io/serviceaccount/";
    }

    public String getNamespace() {
        return "default";
    }

    public String getServiceName() {
        return "";
    }

    public String getServiceAccountToken() throws IOException {

        String path = getServiceAccountPath() + "token";

        File file = new File(path);
        byte[] token = new byte[4096];
        try (FileInputStream in = new FileInputStream(file); ) {
            int readBytes = in.read(token);
            if (readBytes > 4096) {
                throw new RuntimeException("ServiceAccount token too long");
            }
        }
        return new String(token, StandardCharsets.UTF_8);
    }

    public File getServiceAccountCa() {
        return new File(getServiceAccountPath() + "/ca.crt");
    }

    public int apiClientConnectTimeout() {
        return 10000;
    }

    public int apiClientReadTimeout() {
        return 30000;
    }
}
