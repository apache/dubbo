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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.MergeableCluster;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.BROADCAST_CLUSTER;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.rpc.Constants.GENERIC_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_PROTOCOL;
import static org.apache.dubbo.rpc.Constants.SCOPE_KEY;
import static org.apache.dubbo.rpc.Constants.SCOPE_LOCAL;
import static org.apache.dubbo.rpc.Constants.SCOPE_REMOTE;

/**
 * InjvmProtocol
 */
public class InjvmProtocol extends AbstractProtocol {

    public static final String NAME = LOCAL_PROTOCOL;

    public static final int DEFAULT_PORT = 0;

    public static InjvmProtocol getInjvmProtocol(ScopeModel scopeModel) {
        return (InjvmProtocol) scopeModel.getExtensionLoader(Protocol.class).getExtension(InjvmProtocol.NAME, false);
    }

    static Exporter<?> getExporter(Map<String, Exporter<?>> map, URL key) {
        Exporter<?> result = null;

        if (!key.getServiceKey().contains("*")) {
            result = map.get(key.getServiceKey());
        } else {
            if (CollectionUtils.isNotEmptyMap(map)) {
                for (Exporter<?> exporter : map.values()) {
                    if (UrlUtils.isServiceKeyMatch(key, exporter.getInvoker().getUrl())) {
                        result = exporter;
                        break;
                    }
                }
            }
        }

        if (result == null) {
            return null;
        } else if (ProtocolUtils.isGeneric(
            result.getInvoker().getUrl().getParameter(GENERIC_KEY))) {
            return null;
        } else {
            return result;
        }
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        return new InjvmExporter<T>(invoker, invoker.getUrl().getServiceKey(), exporterMap);
    }

    @Override
    public <T> Invoker<T> protocolBindingRefer(Class<T> serviceType, URL url) throws RpcException {
        // group="a,b" or group="*"
        String group = url.getParameter(GROUP_KEY);
        if (StringUtils.isNotEmpty(group)) {
            if ((COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                return doCreateInvoker(url, Cluster.getCluster(url.getScopeModel(), MergeableCluster.NAME), serviceType);
            }
        }
        Cluster cluster = Cluster.getCluster(url.getScopeModel(), url.getParameter(CLUSTER_KEY));
        return doCreateInvoker(url, cluster, serviceType);
    }

    public boolean isInjvmRefer(URL url) {
        String scope = url.getParameter(SCOPE_KEY);
        // Since injvm protocol is configured explicitly, we don't need to set any extra flag, use normal refer process.
        if (SCOPE_LOCAL.equals(scope) || (url.getParameter(LOCAL_PROTOCOL, false))) {
            // if it's declared as local reference
            // 'scope=local' is equivalent to 'injvm=true', injvm will be deprecated in the future release
            return true;
        } else if (SCOPE_REMOTE.equals(scope)) {
            // it's declared as remote reference
            return false;
        } else if (url.getParameter(GENERIC_KEY, false)) {
            // generic invocation is not local reference
            return false;
        } else if (getExporter(exporterMap, url) != null) {
            // Broadcast cluster means that multiple machines will be called,
            // which is not converted to injvm protocol at this time.
            if (BROADCAST_CLUSTER.equalsIgnoreCase(url.getParameter(CLUSTER_KEY))) {
                return false;
            }
            // by default, go through local reference if there's the service exposed locally
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> ClusterInvoker<T> doCreateInvoker(URL url, Cluster cluster, Class<T> type) {
        StaticDirectory directory = new StaticDirectory(url, getInvokers(exporterMap, url, type));
        return (ClusterInvoker<T>) cluster.join(directory, true);
    }

    private <T> List<Invoker<T>> getInvokers(Map<String, Exporter<?>> map, URL url, Class<T> type) {
        List<Invoker<T>> result = new ArrayList<>();

        if (!url.getServiceKey().contains("*")) {
            Exporter<?> exporter = map.get(url.getServiceKey());
            InjvmInvoker<T> invoker = new InjvmInvoker<>(type, url, url.getServiceKey(), exporter);
            result.add(invoker);
        } else {
            if (CollectionUtils.isNotEmptyMap(map)) {
                for (Exporter<?> exporter : map.values()) {
                    if (UrlUtils.isServiceKeyMatch(url, exporter.getInvoker().getUrl())) {
                        URL providerUrl = exporter.getInvoker().getUrl();
                        URL consumerUrl = url.addParameter(GROUP_KEY, providerUrl.getGroup())
                            .addParameter(VERSION_KEY, providerUrl.getVersion());
                        InjvmInvoker<T> invoker = new InjvmInvoker<>(type, consumerUrl, consumerUrl.getServiceKey(), exporter);
                        result.add(invoker);
                    }
                }
            }
        }

        return result;
    }
}
