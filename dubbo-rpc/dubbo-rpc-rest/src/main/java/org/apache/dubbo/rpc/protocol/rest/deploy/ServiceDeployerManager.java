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
package org.apache.dubbo.rpc.protocol.rest.deploy;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.Invoker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.REST_SERVICE_DEPLOYER_URL_ATTRIBUTE_KEY;
import static org.apache.dubbo.remoting.Constants.PORT_UNIFICATION_NETTY4_SERVER;
import static org.apache.dubbo.remoting.Constants.REST_SERVER;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;

public class ServiceDeployerManager {
    private static final ConcurrentMap<String, ServiceDeployer> serviceDeployers = new ConcurrentHashMap<>();

    public static URL deploy(URL currentURL, ServiceRestMetadata serviceRestMetadata, Invoker invoker) {

        AtomicBoolean isNewCreate = new AtomicBoolean();

        ServiceDeployer newServiceDeployer =
                ConcurrentHashMapUtils.computeIfAbsent(serviceDeployers, currentURL.getAddress(), address -> {
                    ServiceDeployer serviceDeployer = new ServiceDeployer();
                    isNewCreate.set(true);
                    return serviceDeployer;
                });

        // register service
        newServiceDeployer.deploy(serviceRestMetadata, invoker);

        // register exception mapper
        newServiceDeployer.registerExtension(currentURL);

        // passing ServiceDeployer to  PortUnificationServer through URL
        // add attribute for server build

        currentURL = currentURL.putAttribute(REST_SERVICE_DEPLOYER_URL_ATTRIBUTE_KEY, newServiceDeployer);

        // not new URL
        if (!isNewCreate.get()) {
            return currentURL;
        }

        URL tmp = currentURL;
        // adapt to older rest versions
        if (REST_SERVER.contains(tmp.getParameter(SERVER_KEY))) {
            tmp = tmp.addParameter(SERVER_KEY, PORT_UNIFICATION_NETTY4_SERVER);
        }

        return tmp;
    }

    public static void close() {
        serviceDeployers.clear();
    }
}
