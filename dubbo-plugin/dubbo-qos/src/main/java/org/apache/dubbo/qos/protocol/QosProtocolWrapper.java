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
package org.apache.dubbo.qos.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.common.QosConstants;
import org.apache.dubbo.qos.pu.QosWireProtocol;
import org.apache.dubbo.qos.server.Server;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_FAILED_START_SERVER;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_WHITELIST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;

@Activate(order = 200)
public class QosProtocolWrapper implements Protocol, ScopeModelAware {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(QosProtocolWrapper.class);

    private AtomicBoolean hasStarted = new AtomicBoolean(false);

    private Protocol protocol;

    private FrameworkModel frameworkModel;

    public QosProtocolWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        startQosServer(invoker.getUrl());
        return protocol.export(invoker);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        startQosServer(url);
        return protocol.refer(type, url);
    }

    @Override
    public void destroy() {
        protocol.destroy();
        stopServer();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }

    private void startQosServer(URL url) {
        try {
            if (!hasStarted.compareAndSet(false, true)) {
                return;
            }

            boolean qosEnable = url.getParameter(QOS_ENABLE, true);
            WireProtocol qosWireProtocol = frameworkModel.getExtensionLoader(WireProtocol.class).getExtension("qos");
            if (qosWireProtocol != null) {
                ((QosWireProtocol) qosWireProtocol).setQosEnable(qosEnable);
            }
            if (!qosEnable) {
                logger.info("qos won't be started because it is disabled. " +
                    "Please check dubbo.application.qos.enable is configured either in system property, " +
                    "dubbo.properties or XML/spring-boot configuration.");
                return;
            }

            String host = url.getParameter(QOS_HOST);
            int port = url.getParameter(QOS_PORT, QosConstants.DEFAULT_PORT);
            boolean acceptForeignIp = Boolean.parseBoolean(url.getParameter(ACCEPT_FOREIGN_IP, "false"));
            String acceptForeignIpWhitelist = url.getParameter(ACCEPT_FOREIGN_IP_WHITELIST, StringUtils.EMPTY_STRING);
            Server server = frameworkModel.getBeanFactory().getBean(Server.class);

            if (server.isStarted()) {
                return;
            }

            server.setHost(host);
            server.setPort(port);
            server.setAcceptForeignIp(acceptForeignIp);
            server.setAcceptForeignIpWhitelist(acceptForeignIpWhitelist);
            server.start();

        } catch (Throwable throwable) {
            logger.warn(QOS_FAILED_START_SERVER, "", "", "Fail to start qos server: ", throwable);
        }
    }

    /*package*/ void stopServer() {
        if (hasStarted.compareAndSet(true, false)) {
            Server server = frameworkModel.getBeanFactory().getBean(Server.class);
            server.stop();
        }
    }
}
