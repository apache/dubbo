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
package org.apache.dubbo.test.common;

/**
 * Using this class as registry center is not very well because of time-consuming.
 * <p>The alternative is to use {@link org.apache.dubbo.test.common.registrycenter.ZookeeperSingleRegistryCenter}</p> or
 * {@link org.apache.dubbo.test.common.registrycenter.ZookeeperMultipleRegistryCenter}
 * @deprecated
 * @see org.apache.dubbo.test.common.registrycenter.ZookeeperSingleRegistryCenter
 * @see org.apache.dubbo.test.common.registrycenter.ZookeeperMultipleRegistryCenter
 */
@Deprecated
public class ZooKeeperServer {

    private static EmbeddedZooKeeper zookeeper1;
    private static EmbeddedZooKeeper zookeeper2;

    public static void start() {
        if (zookeeper1 == null) {
            zookeeper1 = new EmbeddedZooKeeper(2181, true);
            zookeeper1.start();
        }
        if (zookeeper2 == null) {
            zookeeper2 = new EmbeddedZooKeeper(2182, true);
            zookeeper2.start();
        }
    }

    public static void stop() {
        try {
            if (zookeeper1 != null) {
                zookeeper1.stop();
            }
            if (zookeeper2 != null) {
                zookeeper2.stop();
            }
        } finally {
            zookeeper1 = null;
            zookeeper2 = null;
        }
    }
}
