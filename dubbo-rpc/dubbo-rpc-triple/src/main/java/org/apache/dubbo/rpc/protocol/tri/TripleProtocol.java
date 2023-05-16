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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ExecutorUtil;
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
import org.apache.dubbo.rpc.protocol.tri.service.TriBuiltinService;

import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.rpc.Constants.H2_IGNORE_1_0_0_KEY;
import static org.apache.dubbo.rpc.Constants.H2_RESOLVE_FALLBACK_TO_DEFAULT_KEY;
import static org.apache.dubbo.config.Constants.SERVER_THREAD_POOL_NAME;
import static org.apache.dubbo.rpc.Constants.H2_SUPPORT_NO_LOWER_HEADER_KEY;

public class TripleProtocol extends AbstractProtocol {


    public static final String METHOD_ATTR_PACK = "pack";
    private static final Logger logger = LoggerFactory.getLogger(TripleProtocol.class);
    private final PathResolver pathResolver;
    private final TriBuiltinService triBuiltinService;
    private final String acceptEncodings;

    /**
     * There is only one
     */
    public static boolean CONVERT_NO_LOWER_HEADER = false;

    public static boolean IGNORE_1_0_0_VERSION = false;

    public static boolean RESOLVE_FALLBACK_TO_DEFAULT = false;

    public TripleProtocol(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.triBuiltinService = new TriBuiltinService(frameworkModel);
        this.pathResolver = frameworkModel.getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
        CONVERT_NO_LOWER_HEADER = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel())
            .getBoolean(H2_SUPPORT_NO_LOWER_HEADER_KEY, true);
        IGNORE_1_0_0_VERSION = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel())
            .getBoolean(H2_IGNORE_1_0_0_KEY, false);
        RESOLVE_FALLBACK_TO_DEFAULT = ConfigurationUtils.getEnvConfiguration(ApplicationModel.defaultModel())
            .getBoolean(H2_RESOLVE_FALLBACK_TO_DEFAULT_KEY, false);
        Set<String> supported = frameworkModel.getExtensionLoader(DeCompressor.class)
            .getSupportedExtensions();
        this.acceptEncodings = String.join(",", supported);
    }

    @Override
    public int getDefaultPort() {
        return 50051;
    }


    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        String key = serviceKey(url);
        final AbstractExporter<T> exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void afterUnExport() {
                pathResolver.remove(url.getServiceKey());
                pathResolver.remove(url.getServiceModel().getServiceModel().getInterfaceName());
                // set service status
                if (triBuiltinService.enable()) {
                    triBuiltinService.getHealthStatusManager()
                        .setStatus(url.getServiceKey(), ServingStatus.NOT_SERVING);
                    triBuiltinService.getHealthStatusManager()
                        .setStatus(url.getServiceInterface(), ServingStatus.NOT_SERVING);
                }
                exporterMap.remove(key);
            }
        };

        exporterMap.put(key, exporter);

        invokers.add(invoker);

        pathResolver.add(url.getServiceKey(), invoker);
        if (RESOLVE_FALLBACK_TO_DEFAULT) {
            pathResolver.add(url.getServiceModel().getServiceModel().getInterfaceName(), invoker);
        }

        // set service status
        if (triBuiltinService.enable()) {
            triBuiltinService.getHealthStatusManager()
                .setStatus(url.getServiceKey(), HealthCheckResponse.ServingStatus.SERVING);
            triBuiltinService.getHealthStatusManager()
                .setStatus(url.getServiceInterface(), HealthCheckResponse.ServingStatus.SERVING);
        }
        // init
        ExecutorRepository.getInstance(url.getOrDefaultApplicationModel()).createExecutorIfAbsent(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME));

        PortUnificationExchanger.bind(url, new DefaultPuHandler());
        optimizeSerialization(url);
        return exporter;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        optimizeSerialization(url);
        ExecutorService streamExecutor = getOrCreateStreamExecutor(
            url.getOrDefaultApplicationModel(), url);
        AbstractConnectionClient connectionClient = PortUnificationExchanger.connect(url, new DefaultPuHandler());
        TripleInvoker<T> invoker = new TripleInvoker<>(type, url, acceptEncodings,
            connectionClient, invokers, streamExecutor);
        invokers.add(invoker);
        return invoker;
    }

    private ExecutorService getOrCreateStreamExecutor(ApplicationModel applicationModel, URL url) {
        ExecutorService executor = ExecutorRepository.getInstance(applicationModel).createExecutorIfAbsent(ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME));
        Objects.requireNonNull(executor,
            String.format("No available executor found in %s", url));
        return executor;
    }

    @Override
    protected <T> Invoker<T> protocolBindingRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }

    @Override
    public void destroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Destroying protocol [" + this.getClass().getSimpleName() + "] ...");
        }
        PortUnificationExchanger.close();
        pathResolver.destroy();
        super.destroy();
    }
}
