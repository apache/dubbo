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
package org.apache.dubbo.xds.listener;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.XdsException;
import org.apache.dubbo.xds.security.authn.DownstreamTlsConfig;
import org.apache.dubbo.xds.security.authn.GeneralTlsConfig;
import org.apache.dubbo.xds.security.authn.TlsResourceResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext;

@Activate
public class DownstreamTlsConfigListener implements LdsListener {

    protected static final String TLS = "tls";

    protected static final String LDS_VIRTUAL_INBOUND = "virtualInbound";

    protected static final String DOWNSTREAM_TLS_CONTEXT_TYPE =
            "type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext";

    protected static final String TRANSPORT_SOCKET_NAME_TLS = "envoy.transport_sockets.tls";

    protected static final String TRANSPORT_SOCKET_NAME_PLAINTEXT = "envoy.transport_sockets.raw_buffer";

    private final XdsTlsConfigRepository repo;

    public DownstreamTlsConfigListener(ApplicationModel applicationModel) {
        this.repo = applicationModel.getBeanFactory().getOrRegisterBean(XdsTlsConfigRepository.class);
    }

    @Override
    public void onResourceUpdate(List<Listener> listeners) {
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        Map<String, DownstreamTlsConfig> downstreamConfigs = new HashMap<>(4);
        for (Listener listener : listeners) {
            // only choose inbound listeners
            if (!LDS_VIRTUAL_INBOUND.equals(listener.getName())) {
                continue;
            }
            try {
                int port = listener.getAddress().getSocketAddress().getPortValue();
                List<FilterChain> filterChains = listener.getFilterChainsList();
                boolean supportTls = false;
                boolean supportPlainText = false;
                DownstreamTlsConfig downstreamTlsConfig = null;

                for (FilterChain filterChain : filterChains) {
                    if (TRANSPORT_SOCKET_NAME_TLS.equals(
                            filterChain.getTransportSocket().getName())) {
                        supportTls = true;
                    }

                    if (TRANSPORT_SOCKET_NAME_PLAINTEXT.equals(
                            filterChain.getTransportSocket().getName())) {
                        supportPlainText = true;
                    }

                    Any any = filterChain.getTransportSocket().getTypedConfig();

                    if (DOWNSTREAM_TLS_CONTEXT_TYPE.equals(any.getTypeUrl())) {
                        DownstreamTlsContext downstreamTlsContext;
                        try {
                            downstreamTlsContext = any.unpack(DownstreamTlsContext.class);
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }

                        CommonTlsContext commonTlsContext = downstreamTlsContext.getCommonTlsContext();
                        GeneralTlsConfig tlsConfig =
                                TlsResourceResolver.resolveCommonTlsConfig(String.valueOf(port), commonTlsContext);

                        downstreamTlsConfig = new DownstreamTlsConfig(
                                tlsConfig,
                                downstreamTlsContext
                                        .getRequireClientCertificate()
                                        .getValue(),
                                downstreamTlsContext.getRequireSni().getValue(),
                                downstreamTlsContext.getSessionTimeout().getNanos());
                        downstreamConfigs.put(String.valueOf(port), downstreamTlsConfig);
                        break;
                    }
                }

                if (downstreamTlsConfig == null) {
                    downstreamConfigs.put(String.valueOf(port), new DownstreamTlsConfig(TlsType.DISABLE));
                } else {
                    if (supportTls && supportPlainText) {
                        downstreamTlsConfig.setTlsType(TlsType.PERMISSIVE);
                    }
                    if (!supportTls) {
                        downstreamTlsConfig.setTlsType(TlsType.DISABLE);
                    }
                    if (supportTls && !supportPlainText) {
                        downstreamTlsConfig.setTlsType(TlsType.STRICT);
                    }
                }
            } catch (Exception e) {
                throw new XdsException(
                        XdsException.Type.LDS,
                        "Invalid UpstreamTlsContext config provided for port:"
                                + listener.getAddress().getSocketAddress().getPortValue(),
                        e);
            }
            repo.updateInbound(downstreamConfigs);
        }
    }

    public enum TlsType {
        STRICT(0, "Strict Mode"),
        PERMISSIVE(1, "Permissive Mode"),
        DISABLE(2, "Disable Mode"),
        ;
        public static Map<Integer, TlsType> map = new HashMap<>();

        static {
            for (TlsType tlsEnum : TlsType.values()) {
                map.put(tlsEnum.code, tlsEnum);
            }
        }

        private int code;
        private String msg;

        TlsType(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public static TlsType getFromCode(int code) {
            return map.get(code);
        }

        @Override
        public String toString() {
            return "TlsType{" + "code=" + code + ", msg='" + msg + '\'' + '}';
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
