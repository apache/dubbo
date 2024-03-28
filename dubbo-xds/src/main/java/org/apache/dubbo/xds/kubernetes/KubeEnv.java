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

import org.apache.dubbo.common.io.Bytes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class KubeEnv {

    private String apiServerPath = "https://127.0.0.1:6443";
        // "https://kubernetes.default.svc";

    private boolean enableSsl = true;

    private String serviceAccountPath = "/Users/nameles/Desktop/test_secrets/kubernetes.io/serviceaccount";
        // "/var/run/secrets/kubernetes.io/serviceaccount/";

    private String namespace = "default";

    private String serviceName = "";

    private int apiClientConnectTimeout = 10000;

    private int apiClientReadTimeout = 30000;

    public String getApiServerPath() {
        return apiServerPath;
    }

    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceAccountToken() throws IOException {

        String path = getServiceAccountPath() + "/token";

        File file = new File(path);
        byte[] token = new byte[4096];
        if(!file.exists()){
            return "";
        }
        try (FileInputStream in = new FileInputStream(file); ) {
            int readBytes = in.read(token);
            if (readBytes > 4096) {
                throw new RuntimeException("ServiceAccount token too long");
            }
            token = Bytes.copyOf(token,readBytes);
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

    public boolean isEnableSsl() {
        return enableSsl;
    }

    public int getApiClientConnectTimeout() {
        return apiClientConnectTimeout;
    }

    public int getApiClientReadTimeout() {
        return apiClientReadTimeout;
    }

    public void setApiServerPath(String apiServerPath) {
        this.apiServerPath = apiServerPath;
    }

    public void setEnableSsl(boolean enableSsl) {
        this.enableSsl = enableSsl;
    }

    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setApiClientConnectTimeout(int apiClientConnectTimeout) {
        this.apiClientConnectTimeout = apiClientConnectTimeout;
    }

    public void setApiClientReadTimeout(int apiClientReadTimeout) {
        this.apiClientReadTimeout = apiClientReadTimeout;
    }
}
