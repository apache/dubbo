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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.protocol.tri.compressor.DeCompressor;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.DefaultRequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMappingRegistry;
import org.apache.dubbo.rpc.protocol.tri.service.TriBuiltinService;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.config.Constants.CLIENT_THREAD_POOL_NAME;
import static org.apache.dubbo.config.Constants.SERVER_THREAD_POOL_NAME;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_IGNORE_1_0_0_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_OPENAPI_ENABLED;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_RESOLVE_FALLBACK_TO_DEFAULT_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_REST_ENABLED;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_SUPPORT_NO_LOWER_HEADER_KEY;
import static org.apache.dubbo.rpc.Constants.H2_SETTINGS_VERBOSE_ENABLED;

public class TripleProtocol extends AbstractProtocol {

    private final PathResolver pathResolver;
    private final RequestMappingRegistry mappingRegistry;
    private final TriBuiltinService triBuiltinService;
    private final String acceptEncodings;

    public static boolean CONVERT_NO_LOWER_HEADER = true;
    public static boolean IGNORE_1_0_0_VERSION = false;
    public static boolean RESOLVE_FALLBACK_TO_DEFAULT = true;
    public static boolean VERBOSE_ENABLED = false;
    public static boolean REST_ENABLED = true;
    public static boolean OPENAPI_ENABLED = false;

    public TripleProtocol(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        triBuiltinService = new TriBuiltinService(frameworkModel);
        pathResolver = frameworkModel.getDefaultExtension(PathResolver.class);
        mappingRegistry = frameworkModel.getOrRegisterBean(DefaultRequestMappingRegistry.class);
        acceptEncodings = String.join(",", frameworkModel.getSupportedExtensions(DeCompressor.class));

        // init env settings
        Configuration conf = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel());
        CONVERT_NO_LOWER_HEADER = conf.getBoolean(H2_SETTINGS_SUPPORT_NO_LOWER_HEADER_KEY, true);
        IGNORE_1_0_0_VERSION = conf.getBoolean(H2_SETTINGS_IGNORE_1_0_0_KEY, false);
        RESOLVE_FALLBACK_TO_DEFAULT = conf.getBoolean(H2_SETTINGS_RESOLVE_FALLBACK_TO_DEFAULT_KEY, true);

        // init global settings
        Configuration globalConf = ConfigurationUtils.getGlobalConfiguration(frameworkModel.defaultApplication());
        VERBOSE_ENABLED = globalConf.getBoolean(H2_SETTINGS_VERBOSE_ENABLED, false);
        REST_ENABLED = globalConf.getBoolean(H2_SETTINGS_REST_ENABLED, true);
        OPENAPI_ENABLED = globalConf.getBoolean(H2_SETTINGS_OPENAPI_ENABLED, false);

        ServletExchanger.init(globalConf);
        Http3Exchanger.init(globalConf);
    }

    @Override
    public int getDefaultPort() {
        return 50051;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        String key = serviceKey(url);

        // create exporter
        AbstractExporter<T> exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void afterUnExport() {
                // unregister grpc request mapping
                pathResolver.unregister(invoker);

                // unregister rest request mapping
                if (REST_ENABLED) {
                    mappingRegistry.unregister(invoker);
                }

                // set service status to NOT_SERVING
                setServiceStatus(url, false);

                exporterMap.remove(key);
            }
        };
        exporterMap.put(key, exporter);

        // add invoker
        invokers.add(invoker);

        // register grpc path mapping
        pathResolver.register(invoker);

        // register rest request mapping
        if (REST_ENABLED) {
            mappingRegistry.register(invoker);
        }

        // set service status to SERVING
        setServiceStatus(url, true);

        // init server executor
        ExecutorRepository.getInstance(url.getOrDefaultApplicationModel())
                .createExecutorIfAbsent(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME));

        // bind server port
        bindServerPort(url);

        // optimize serialization
        optimizeSerialization(url);

        return exporter;
    }

    private void setServiceStatus(URL url, boolean serving) {
        if (triBuiltinService.enable()) {
            ServingStatus status = serving ? ServingStatus.SERVING : ServingStatus.NOT_SERVING;
            triBuiltinService.getHealthStatusManager().setStatus(url.getServiceKey(), status);
            triBuiltinService.getHealthStatusManager().setStatus(url.getServiceInterface(), status);
        }
    }

    private void bindServerPort(URL url) {
        boolean bindPort = true;

        if (ServletExchanger.isEnabled()) {
            int port = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
            Integer serverPort = ServletExchanger.getServerPort();
            if (serverPort == null) {
                if (NetUtils.isPortInUsed(port)) {
                    bindPort = false;
                }
            } else if (serverPort == port) {
                bindPort = false;
            }
            ServletExchanger.bind(url);
        }

        if (bindPort) {
            PortUnificationExchanger.bind(url, new DefaultPuHandler());
        }

        Http3Exchanger.bind(url);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        optimizeSerialization(url);
        ExecutorService streamExecutor = getOrCreateStreamExecutor(url.getOrDefaultApplicationModel(), url);
        AbstractConnectionClient connectionClient = Http3Exchanger.isEnabled(url)
                ? Http3Exchanger.connect(url)
                : PortUnificationExchanger.connect(url, new DefaultPuHandler());
        TripleInvoker<T> invoker =
                new TripleInvoker<>(type, url, acceptEncodings, connectionClient, invokers, streamExecutor);
        invokers.add(invoker);
        return invoker;
    }

    private ExecutorService getOrCreateStreamExecutor(ApplicationModel applicationModel, URL url) {
        url = url.addParameter(THREAD_NAME_KEY, CLIENT_THREAD_POOL_NAME)
                .addParameterIfAbsent(THREADPOOL_KEY, DEFAULT_CLIENT_THREADPOOL);
        ExecutorService executor =
                ExecutorRepository.getInstance(applicationModel).createExecutorIfAbsent(url);
        Objects.requireNonNull(executor, String.format("No available executor found in %s", url));
        return executor;
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }

    @Override
    public void destroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Destroying protocol [{}] ...", getClass().getSimpleName());
        }
        PortUnificationExchanger.close();
        Http3Exchanger.close();
        pathResolver.destroy();
        if (REST_ENABLED) {
            mappingRegistry.destroy();
        }
        super.destroy();
    }
}
