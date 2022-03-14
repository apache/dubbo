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

import com.google.protobuf.ByteString;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.ConnectionManager;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.AbstractExporter;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.service.TriBuiltinService;

import grpc.health.v1.HealthCheckResponse;
import org.apache.dubbo.triple.TripleWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CLIENT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

public class TripleProtocol extends AbstractProtocol {
    private static final String CLIENT_THREAD_POOL_NAME = "DubboTriClientHandler";

    private static final Logger logger = LoggerFactory.getLogger(TripleProtocol.class);
    private final PathResolver pathResolver;
    private final TriBuiltinService triBuiltinService;
    private final ConnectionManager connectionManager;
    private final FrameworkModel frameworkModel;
    private boolean versionChecked = false;

    public TripleProtocol(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        this.triBuiltinService = new TriBuiltinService(frameworkModel);
        this.pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
        this.connectionManager = frameworkModel.getExtensionLoader(ConnectionManager.class).getExtension("multiple");
    }

    @Override
    public int getDefaultPort() {
        return 50051;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
        checkProtobufVersion(url);
        String key = serviceKey(url);
        final AbstractExporter<T> exporter = new AbstractExporter<T>(invoker) {
            @Override
            public void afterUnExport() {
                pathResolver.remove(url.getServiceKey());
                pathResolver.remove(url.getServiceInterface());
                exporterMap.remove(key);
            }
        };

        exporterMap.put(key, exporter);

        invokers.add(invoker);

        pathResolver.add(url.getServiceKey(), invoker);
        pathResolver.add(url.getServiceInterface(), invoker);

        // set service status
        triBuiltinService.getHealthStatusManager().setStatus(url.getServiceKey(), HealthCheckResponse.ServingStatus.SERVING);
        triBuiltinService.getHealthStatusManager().setStatus(url.getServiceInterface(), HealthCheckResponse.ServingStatus.SERVING);

        PortUnificationExchanger.bind(invoker.getUrl());
        return exporter;
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        final MultipleSerialization serialization = frameworkModel
            .getExtensionLoader(MultipleSerialization.class)
            .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY));

        url = url.addParameter(THREAD_NAME_KEY, CLIENT_THREAD_POOL_NAME);
        url = url.addParameterIfAbsent(THREADPOOL_KEY, DEFAULT_CLIENT_THREADPOOL);
        url.getOrDefaultApplicationModel()
            .getExtensionLoader(ExecutorRepository.class)
            .getDefaultExtension()
            .createExecutorIfAbsent(url);
        // TODO support config
//        String compressorStr = ConfigurationUtils.getCachedDynamicProperty(frameworkModel, COMPRESSOR_KEY, Identity.MESSAGE_ENCODING);
//        Compressor defaultCompressor = Compressor.getCompressor(frameworkModel, compressorStr);
        Compressor defaultCompressor = Compressor.NONE;
//        String acceptEncoding = Compressor.getAcceptEncoding(frameworkModel);
        String acceptEncoding = Compressor.NONE.getMessageEncoding();
        final String serializationName = url.getParameter(org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY, org.apache.dubbo.remoting.Constants.DEFAULT_REMOTING_SERIALIZATION);
        TripleInvoker<T> invoker;
        try {
            invoker = new TripleInvoker<>(type, url, serialization, serializationName, defaultCompressor, acceptEncoding, connectionManager, invokers);
        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
        invokers.add(invoker);
        return invoker;
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

    private void checkProtobufVersion(URL url) {
        if (versionChecked) {
            return;
        }
        if (url.getServiceModel() == null) {
            return;
        }
        ServiceDescriptor descriptor = url.getServiceModel().getServiceModel();
        if (descriptor == null) {
            return;
        }
        if (descriptor.getAllMethods().stream().noneMatch(MethodDescriptor::isNeedWrap)) {
            return;
        }

        TripleWrapper.TripleResponseWrapper responseWrapper = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setData(ByteString.copyFromUtf8("Test"))
            .setSerializeType("Test")
            .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            responseWrapper.writeTo(baos);
        } catch (IOException e) {
            throw new IllegalStateException("Bad protobuf-java version detected! Please make sure the version of user's " +
                "classloader is greater than 3.11.0 ", e);
        }
        this.versionChecked = true;
    }
}
