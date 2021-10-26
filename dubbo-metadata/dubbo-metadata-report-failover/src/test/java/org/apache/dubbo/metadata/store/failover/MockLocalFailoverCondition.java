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

/**
 * @author yiji@apache.org
 */
public class MockLocalFailoverCondition implements FailoverCondition {

    @Override
    public boolean shouldRegister(URL url) {
        // we just register same datacenter.
        return isLocalDataCenter(url);
    }

    @Override
    public boolean shouldQuery(URL url) {
        // we want read any metadata report server.
        return true;
    }

    @Override
    public boolean isLocalDataCenter(URL url) {
        // we mock current datacenter is `127.0.0.1:2181`
        String current = "127.0.0.1:2181";
        return url.getBackupAddress().contains(current);
    }

}