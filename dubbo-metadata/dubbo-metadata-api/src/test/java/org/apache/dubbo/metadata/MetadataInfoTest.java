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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Test;

public class MetadataInfoTest {
    @Test
    public void revisionTest() {
        MetadataInfo metadataInfo = new MetadataInfo("demo");
        metadataInfo.setApp("demo");

        URL url = URL.valueOf("dubbo://10.230.11.211:20880/org.apache.dubbo.metadata.DemoService?timeout=1000&testKey=aaa");
        MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo(url);
        metadataInfo.addService(serviceInfo);

        System.out.println(serviceInfo.toDescString());
        System.out.println(metadataInfo.calAndGetRevision());
    }
}
