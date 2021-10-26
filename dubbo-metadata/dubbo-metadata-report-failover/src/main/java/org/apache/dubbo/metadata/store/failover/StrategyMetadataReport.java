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
package org.apache.dubbo.metadata.store.failover;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metadata.report.MetadataReport;

/**
 * @author yiji@apache.org
 */
public abstract class StrategyMetadataReport implements MetadataReport {

    // failover configured url, eg: failover://127.0.1:2181?backup=localhost:2181|localhost:2181
    protected URL url;

    protected static final String STRATEGY_KEY = "strategy";

    // proxy metadata report strategy, used to decide whether to write or read metadata
    protected FailoverCondition strategy;

    protected ExtensionLoader<FailoverCondition> failoverLoader = ExtensionLoader.getExtensionLoader(FailoverCondition.class);

    public StrategyMetadataReport(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url is required.");
        }
        this.url = url;
        createFailoverStrategy(url);
    }

    protected void createFailoverStrategy(URL url) {
        String strategy = url.getParameter(STRATEGY_KEY);
        if (strategy != null) {
            if (!failoverLoader.hasExtension(strategy)) {
                throw new IllegalArgumentException("No '" + strategy + "' failover condition extension found.");
            }
            this.strategy = failoverLoader.getExtension(strategy);
        }
    }

    /**
     * Whether metadata should be reported.
     *
     * @param url registry url, eg: zookeeper://127.0.0.1:2181
     * @return true store metadata to the specified URL.
     */
    protected boolean shouldRegister(URL url) {
        return this.strategy == null || this.strategy.shouldRegister(url);
    }

    /**
     * Whether metadata should be read from specified url.
     *
     * @param url registry url, eg: zookeeper://127.0.0.1:2181
     * @return true read metadata from specified URL.
     */
    protected boolean shouldQuery(URL url) {
        return this.strategy == null || this.strategy.shouldQuery(url);
    }

    /**
     * Judge whether it is a local region or a local datacenter.
     * <p>
     * Allows the local region or datacenter to be read first.
     *
     * @param url
     * @return
     */
    protected boolean isLocalDataCenter(URL url) {
        return this.strategy == null || this.strategy.isLocalDataCenter(url);
    }

}