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
package org.apache.dubbo.servicedata.store.test;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.servicedata.support.AbstractServiceStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZookeeperRegistry
 */
public class JTestServiceStore4Test extends AbstractServiceStore {

    private final static Logger logger = LoggerFactory.getLogger(JTestServiceStore4Test.class);



    public JTestServiceStore4Test(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
    }

    public Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    protected void doPutService(URL url) {
        store.put(getKey(url), url.toParameterString());
    }

    @Override
    protected URL doPeekService(URL url) {
        String queryV = store.get(getKey(url));
        return url.clearParameters().addParameterString(queryV);
    }

    private static String getProtocol(URL url) {
        String protocol = url.getParameter(Constants.SIDE_KEY);
        protocol = protocol == null ? url.getProtocol() : protocol;
        return protocol;
    }

    public static String getKey(URL url){
        return getProtocol(url) + url.getServiceKey();
    }

}
