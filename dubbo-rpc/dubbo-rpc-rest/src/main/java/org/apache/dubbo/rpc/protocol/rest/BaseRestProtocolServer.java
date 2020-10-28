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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import org.jboss.resteasy.spi.ResteasyDeployment;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXTENSION_KEY;

public abstract class BaseRestProtocolServer implements RestProtocolServer {

    private String address;

    @Override
    public void start(URL url) {
        getDeployment().getMediaTypeMappings().put("json", "application/json");
        getDeployment().getMediaTypeMappings().put("xml", "text/xml");
//        server.getDeployment().getMediaTypeMappings().put("xml", "application/xml");
        getDeployment().getProviderClasses().add(RpcContextFilter.class.getName());
        // TODO users can override this mapper, but we just rely on the current priority strategy of resteasy
        getDeployment().getProviderClasses().add(RpcExceptionMapper.class.getName());

        loadProviders(url.getParameter(EXTENSION_KEY, ""));

        doStart(url);
    }

    @Override
    public void deploy(Class resourceDef, Object resourceInstance, String contextPath) {
        if (StringUtils.isEmpty(contextPath)) {
            getDeployment().getRegistry().addResourceFactory(new DubboResourceFactory(resourceInstance, resourceDef));
        } else {
            getDeployment().getRegistry().addResourceFactory(new DubboResourceFactory(resourceInstance, resourceDef), contextPath);
        }
    }

    @Override
    public void undeploy(Class resourceDef) {
        getDeployment().getRegistry().removeRegistrations(resourceDef);
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    protected void loadProviders(String value) {
        for (String clazz : COMMA_SPLIT_PATTERN.split(value)) {
            if (!StringUtils.isEmpty(clazz)) {
                getDeployment().getProviderClasses().add(clazz.trim());
            }
        }
    }

    protected abstract ResteasyDeployment getDeployment();

    protected abstract void doStart(URL url);
}
