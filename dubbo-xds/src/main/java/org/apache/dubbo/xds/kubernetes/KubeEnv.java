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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.istio.XdsEnv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class KubeEnv implements XdsEnv {

    private String apiServerPath;

    private Boolean enableSsl;

    private String serviceAccountCaPath;

    private String serviceAccountTokenPath;

    private String namespace;

    private String serviceName;

    private String cluster;

    private Integer apiClientConnectTimeout;

    private Integer apiClientReadTimeout;

    public KubeEnv(ApplicationModel applicationModel) {
        // get config from applicationModel ...
        setDefault();
    }

    public void setDefault() {
        if (StringUtils.isEmpty(apiServerPath)) {
            apiServerPath = getStringProp("API_SERVER_PATH", "https://kubernetes.default.svc");
        }
        if (enableSsl != null) {
            enableSsl = true;
        }
        if (StringUtils.isEmpty(serviceAccountCaPath)) {
            serviceAccountCaPath = getStringProp("SA_CA_PATH", "/Users/smzdm/hjf/xds/resources/ca.crt");
        }
        if (StringUtils.isEmpty(serviceAccountTokenPath)) {
            serviceAccountTokenPath = getStringProp("SA_TOKEN_PATH", "/Users/smzdm/hjf/xds/resources/token");
        }
        if (StringUtils.isEmpty(namespace)) {
            namespace = getStringProp("NAMESPACE", "dubbo-demo");
        }
        if (StringUtils.isEmpty(serviceName)) {
            serviceName = getStringProp("SERVICE_NAME", "");
        }
        if (apiClientConnectTimeout == null) {
            apiClientConnectTimeout = getIntProp("API_CLIENT_CONNECT_TIMEOUT", "10000");
        }
        if (apiClientReadTimeout == null) {
            apiClientReadTimeout = getIntProp("API_CLIENT_READ_TIMEOUT", "30000");
        }
        if (StringUtils.isEmpty(cluster)) {
            cluster = getStringProp("CLUSTER", "cluster.local");
        }
        if (enableSsl == null) {
            enableSsl = true;
        }
    }

    public String getApiServerPath() {
        return apiServerPath;
    }

    public String getServiceAccountCaPath() {
        return serviceAccountCaPath;
    }

    public String getServiceAccountTokenPath() {
        return serviceAccountTokenPath;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getServiceName() {
        return serviceName;
    }

    public byte[] getServiceAccountToken() throws IOException {
        return readFileAsBytes(getServiceAccountTokenPath());
    }

    public byte[] getServiceAccountCa() throws IOException {
        return readFileAsBytes(getServiceAccountCaPath());
    }

    private byte[] readFileAsBytes(String path) throws IOException {
        File file = new File(path);
        byte[] value = new byte[4096];
        if (!file.exists()) {
            return new byte[0];
        }
        try (FileInputStream in = new FileInputStream(file); ) {
            int readBytes = in.read(value);
            if (readBytes > 4096) {
                throw new RuntimeException("Security resource size > 4096: Too long");
            }
            value = Bytes.copyOf(value, readBytes);
        }

        return value;
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

    public void setServiceAccountCaPath(String serviceAccountPath) {
        this.serviceAccountCaPath = serviceAccountPath;
    }

    public void setServiceAccountTokenPath(String serviceAccountTokenPath) {
        this.serviceAccountTokenPath = serviceAccountTokenPath;
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

    @Override
    public String getCluster() {
        return cluster;
    }
}
