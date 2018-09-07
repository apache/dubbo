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
package org.apache.dubbo.servicedata.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class AbstractServiceStoreTest {

    private AbstractServiceStore abstractServiceStore;
    private URL url;

    @Before
    public void before(){
        url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444");

        abstractServiceStore = new AbstractServiceStore(url) {
            @Override
            protected void doPutService(URL url) {

            }

            @Override
            protected void doRemoveService(URL url) {

            }

            @Override
            protected URL doPeekService(URL url) {
                return null;
            }
        };

    }


}
