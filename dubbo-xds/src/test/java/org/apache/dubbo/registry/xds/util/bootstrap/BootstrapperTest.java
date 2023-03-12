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
package org.apache.dubbo.registry.xds.util.bootstrap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.registry.xds.XdsInitializationException;

import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class BootstrapperTest {
    @Test
    void testParse() throws XdsInitializationException {
        String rawData = "{\n" +
            "  \"xds_servers\": [\n" +
            "    {\n" +
            "      \"server_uri\": \"unix:///etc/istio/proxy/XDS\",\n" +
            "      \"channel_creds\": [\n" +
            "        {\n" +
            "          \"type\": \"insecure\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"server_features\": [\n" +
            "        \"xds_v3\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"node\": {\n" +
            "    \"id\": \"sidecar~172.17.0.4~dubbo-demo-consumer-deployment-grpc-agent-58585cb9cd-gp79p.dubbo-demo~dubbo-demo.svc.cluster.local\",\n" +
            "    \"metadata\": {\n" +
            "      \"ANNOTATIONS\": {\n" +
            "        \"inject.istio.io/templates\": \"grpc-agent\",\n" +
            "        \"kubernetes.io/config.seen\": \"2022-07-19T12:53:29.742565722Z\",\n" +
            "        \"kubernetes.io/config.source\": \"api\",\n" +
            "        \"prometheus.io/path\": \"/stats/prometheus\",\n" +
            "        \"prometheus.io/port\": \"15020\",\n" +
            "        \"prometheus.io/scrape\": \"true\",\n" +
            "        \"proxy.istio.io/config\": \"{\\\"holdApplicationUntilProxyStarts\\\": true}\",\n" +
            "        \"proxy.istio.io/overrides\": \"{\\\"containers\\\":[{\\\"name\\\":\\\"app\\\",\\\"image\\\":\\\"gcr.io/istio-testing/app:latest\\\",\\\"args\\\":[\\\"--metrics=15014\\\",\\\"--port\\\",\\\"18080\\\",\\\"--tcp\\\",\\\"19090\\\",\\\"--xds-grpc-server=17070\\\",\\\"--grpc\\\",\\\"17070\\\",\\\"--grpc\\\",\\\"17171\\\",\\\"--port\\\",\\\"3333\\\",\\\"--port\\\",\\\"8080\\\",\\\"--version\\\",\\\"v1\\\",\\\"--crt=/cert.crt\\\",\\\"--key=/cert.key\\\"],\\\"ports\\\":[{\\\"containerPort\\\":17070,\\\"protocol\\\":\\\"TCP\\\"},{\\\"containerPort\\\":17171,\\\"protocol\\\":\\\"TCP\\\"},{\\\"containerPort\\\":8080,\\\"protocol\\\":\\\"TCP\\\"},{\\\"name\\\":\\\"tcp-health-port\\\",\\\"containerPort\\\":3333,\\\"protocol\\\":\\\"TCP\\\"}],\\\"env\\\":[{\\\"name\\\":\\\"INSTANCE_IP\\\",\\\"valueFrom\\\":{\\\"fieldRef\\\":{\\\"apiVersion\\\":\\\"v1\\\",\\\"fieldPath\\\":\\\"status.podIP\\\"}}}],\\\"resources\\\":{},\\\"volumeMounts\\\":[{\\\"name\\\":\\\"kube-api-access-2tknx\\\",\\\"readOnly\\\":true,\\\"mountPath\\\":\\\"/var/run/secrets/kubernetes.io/serviceaccount\\\"}],\\\"livenessProbe\\\":{\\\"tcpSocket\\\":{\\\"port\\\":\\\"tcp-health-port\\\"},\\\"initialDelaySeconds\\\":10,\\\"timeoutSeconds\\\":1,\\\"periodSeconds\\\":10,\\\"successThreshold\\\":1,\\\"failureThreshold\\\":10},\\\"readinessProbe\\\":{\\\"httpGet\\\":{\\\"path\\\":\\\"/\\\",\\\"port\\\":8080,\\\"scheme\\\":\\\"HTTP\\\"},\\\"initialDelaySeconds\\\":1,\\\"timeoutSeconds\\\":1,\\\"periodSeconds\\\":2,\\\"successThreshold\\\":1,\\\"failureThreshold\\\":10},\\\"startupProbe\\\":{\\\"tcpSocket\\\":{\\\"port\\\":\\\"tcp-health-port\\\"},\\\"timeoutSeconds\\\":1,\\\"periodSeconds\\\":10,\\\"successThreshold\\\":1,\\\"failureThreshold\\\":10},\\\"terminationMessagePath\\\":\\\"/dev/termination-log\\\",\\\"terminationMessagePolicy\\\":\\\"File\\\",\\\"imagePullPolicy\\\":\\\"Always\\\",\\\"securityContext\\\":{\\\"runAsUser\\\":1338,\\\"runAsGroup\\\":1338}},{\\\"name\\\":\\\"dubbo-demo-consumer\\\",\\\"image\\\":\\\"dockeddocking/dubbo:consumer.v1.0\\\",\\\"command\\\":[\\\"sh\\\",\\\"-c\\\",\\\"java $JAVA_OPTS -jar dubbo-demo-consumer.jar \\\"],\\\"resources\\\":{},\\\"volumeMounts\\\":[{\\\"name\\\":\\\"kube-api-access-2tknx\\\",\\\"readOnly\\\":true,\\\"mountPath\\\":\\\"/var/run/secrets/kubernetes.io/serviceaccount\\\"}],\\\"terminationMessagePath\\\":\\\"/dev/termination-log\\\",\\\"terminationMessagePolicy\\\":\\\"File\\\",\\\"imagePullPolicy\\\":\\\"Always\\\"}]}\",\n" +
            "        \"sidecar.istio.io/rewriteAppHTTPProbers\": \"false\",\n" +
            "        \"sidecar.istio.io/status\": \"{\\\"initContainers\\\":null,\\\"containers\\\":[\\\"app\\\",\\\"dubbo-demo-consumer\\\",\\\"istio-proxy\\\"],\\\"volumes\\\":[\\\"workload-socket\\\",\\\"workload-certs\\\",\\\"istio-xds\\\",\\\"istio-data\\\",\\\"istio-podinfo\\\",\\\"istio-token\\\",\\\"istiod-ca-cert\\\"],\\\"imagePullSecrets\\\":null,\\\"revision\\\":\\\"default\\\"}\"\n" +
            "      },\n" +
            "      \"APP_CONTAINERS\": \"app,dubbo-demo-consumer\",\n" +
            "      \"CLUSTER_ID\": \"Kubernetes\",\n" +
            "      \"ENVOY_PROMETHEUS_PORT\": 15090,\n" +
            "      \"ENVOY_STATUS_PORT\": 15021,\n" +
            "      \"GENERATOR\": \"grpc\",\n" +
            "      \"INSTANCE_IPS\": \"172.17.0.4\",\n" +
            "      \"INTERCEPTION_MODE\": \"REDIRECT\",\n" +
            "      \"ISTIO_PROXY_SHA\": \"2b6009118109b480e1d5abf3188fd7d9c0c0acf0\",\n" +
            "      \"ISTIO_VERSION\": \"1.14.1\",\n" +
            "      \"LABELS\": {\n" +
            "        \"app\": \"dubbo-demo-consumer-dev\",\n" +
            "        \"pod-template-hash\": \"58585cb9cd\",\n" +
            "        \"service.istio.io/canonical-name\": \"dubbo-demo-consumer-dev\",\n" +
            "        \"service.istio.io/canonical-revision\": \"v1\",\n" +
            "        \"version\": \"v1\"\n" +
            "      },\n" +
            "      \"MESH_ID\": \"cluster.local\",\n" +
            "      \"NAME\": \"dubbo-demo-consumer-deployment-grpc-agent-58585cb9cd-gp79p\",\n" +
            "      \"NAMESPACE\": \"dubbo-demo\",\n" +
            "      \"OWNER\": \"kubernetes://apis/apps/v1/namespaces/dubbo-demo/deployments/dubbo-demo-consumer-deployment-grpc-agent\",\n" +
            "      \"PILOT_SAN\": [\n" +
            "        \"istiod.istio-system.svc\"\n" +
            "      ],\n" +
            "      \"POD_PORTS\": \"[{\\\"containerPort\\\":17070,\\\"protocol\\\":\\\"TCP\\\"},{\\\"containerPort\\\":17171,\\\"protocol\\\":\\\"TCP\\\"},{\\\"containerPort\\\":8080,\\\"protocol\\\":\\\"TCP\\\"},{\\\"name\\\":\\\"tcp-health-port\\\",\\\"containerPort\\\":3333,\\\"protocol\\\":\\\"TCP\\\"}]\",\n" +
            "      \"PROV_CERT\": \"var/run/secrets/istio/root-cert.pem\",\n" +
            "      \"PROXY_CONFIG\": {\n" +
            "        \"binaryPath\": \"/usr/local/bin/envoy\",\n" +
            "        \"concurrency\": 2,\n" +
            "        \"configPath\": \"./etc/istio/proxy\",\n" +
            "        \"controlPlaneAuthPolicy\": \"MUTUAL_TLS\",\n" +
            "        \"discoveryAddress\": \"istiod.istio-system.svc:15012\",\n" +
            "        \"drainDuration\": \"45s\",\n" +
            "        \"holdApplicationUntilProxyStarts\": true,\n" +
            "        \"parentShutdownDuration\": \"60s\",\n" +
            "        \"proxyAdminPort\": 15000,\n" +
            "        \"serviceCluster\": \"istio-proxy\",\n" +
            "        \"statNameLength\": 189,\n" +
            "        \"statusPort\": 15020,\n" +
            "        \"terminationDrainDuration\": \"5s\",\n" +
            "        \"tracing\": {\n" +
            "          \"zipkin\": {\n" +
            "            \"address\": \"zipkin.istio-system:9411\"\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"SERVICE_ACCOUNT\": \"default\",\n" +
            "      \"WORKLOAD_NAME\": \"dubbo-demo-consumer-deployment-grpc-agent\"\n" +
            "    },\n" +
            "    \"locality\": {},\n" +
            "    \"UserAgentVersionType\": null\n" +
            "  },\n" +
            "  \"certificate_providers\": {\n" +
            "    \"default\": {\n" +
            "      \"plugin_name\": \"file_watcher\",\n" +
            "      \"config\": {\n" +
            "        \"certificate_file\": \"/var/lib/istio/data/cert-chain.pem\",\n" +
            "        \"private_key_file\": \"/var/lib/istio/data/key.pem\",\n" +
            "        \"ca_certificate_file\": \"/var/lib/istio/data/root-cert.pem\",\n" +
            "        \"refresh_interval\": \"900s\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"server_listener_resource_name_template\": \"xds.istio.io/grpc/lds/inbound/%s\"\n" +
            "}";
        BootstrapperImpl.bootstrapPathFromEnvVar = "";
        BootstrapperImpl bootstrapper = new BootstrapperImpl();
        bootstrapper.setFileReader(createFileReader(rawData));
        Bootstrapper.BootstrapInfo info = bootstrapper.bootstrap();
        List<Bootstrapper.ServerInfo> serverInfoList = info.servers();
        Assertions.assertEquals(serverInfoList.get(0).target(), "unix:///etc/istio/proxy/XDS");
        URLAddress address =URLAddress.parse(serverInfoList.get(0).target(),null, false);
        Assertions.assertEquals(new DomainSocketAddress(address.getPath()).path(), "etc/istio/proxy/XDS");
    }

    @Test
    void testUrl() {
        URL url = URL.valueOf("dubbo://127.0.0.1:23456/TestService?useAgent=true");
        Assertions.assertTrue(url.getParameter("useAgent", false));
    }

    private static BootstrapperImpl.FileReader createFileReader(final String rawData) {
        return new BootstrapperImpl.FileReader() {
            @Override
            public String readFile(String path) {
                return rawData;
            }
        };
    }

}
