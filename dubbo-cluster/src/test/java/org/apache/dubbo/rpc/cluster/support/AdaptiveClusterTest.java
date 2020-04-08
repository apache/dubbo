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
package org.apache.dubbo.rpc.cluster.support;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AdaptiveClusterTest {

    Directory<?> directory1 = mock(Directory.class);
    Directory<?> directory2 = mock(Directory.class);
    Directory<?> directory3 = mock(Directory.class);

    URL url = URL.valueOf("test://test:11/test?cluster=AdaptiveClusterTest");

    @BeforeAll
    public static void setUpExtension() {
        ExtensionLoader<Cluster> extensionLoader = ExtensionLoader.getExtensionLoader(Cluster.class);
        extensionLoader.addExtension("AdaptiveClusterTest", MockCluster.class);
    }

    @Test
    public void testNullDic() {
        AdaptiveCluster adaptiveCluster = new AdaptiveCluster();
        Assertions.assertThrows(RpcException.class, () -> adaptiveCluster.join(null));
    }

    @Test
    public void testJoin() {
        given(directory1.getUrl()).willReturn(url);
        given(directory1.getConsumerUrl()).willReturn(null);

        given(directory2.getUrl()).willReturn(null);
        given(directory2.getConsumerUrl()).willReturn(url);

        given(directory3.getUrl()).willReturn(url);
        given(directory3.getConsumerUrl()).willReturn(url);

        AdaptiveCluster adaptiveCluster = new AdaptiveCluster();
        adaptiveCluster.join(directory1);
        adaptiveCluster.join(directory2);
        adaptiveCluster.join(directory3);
    }
}
