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
package org.apache.dubbo.qos.pu;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.qos.server.DubboLogo;
import org.apache.dubbo.qos.api.QosConfiguration;
import org.apache.dubbo.qos.server.handler.QosProcessHandler;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.AbstractWireProtocol;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.ArrayList;
import java.util.List;

@Activate
public class QosWireProtocol extends AbstractWireProtocol implements ScopeModelAware {

    public QosWireProtocol(FrameworkModel frameworkModel) {
        super(new QosDetector(frameworkModel));
    }

    public void setQosEnable(boolean flag) {
        ((QosDetector)this.detector()).setQosEnableFlag(flag);
    }

    @Override
    public void configServerProtocolHandler(URL url, ChannelOperator operator) {
        // add qosProcess handler
        QosProcessHandler handler = new QosProcessHandler(url.getOrDefaultFrameworkModel(),
            QosConfiguration.builder()
                .welcome(DubboLogo.DUBBO)
                .acceptForeignIp(false)
                .acceptForeignIpWhitelist(StringUtils.EMPTY_STRING)
                .anonymousAccessPermissionLevel(PermissionLevel.PUBLIC.name())
                .build()
        );
        List<ChannelHandler> handlers = new ArrayList<>();
        handlers.add(new ChannelHandlerPretender(handler));
        operator.configChannelHandler(handlers);
    }

}
