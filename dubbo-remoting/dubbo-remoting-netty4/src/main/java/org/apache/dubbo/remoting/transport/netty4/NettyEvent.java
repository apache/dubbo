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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.event.CustomAfterPost;
import org.apache.dubbo.common.event.DubboEvent;
import org.apache.dubbo.rpc.model.ApplicationModel;

public class NettyEvent extends DubboEvent implements CustomAfterPost<NettyEvent.MetricsData> {

    private MetricsData postResult;

    public NettyEvent(ApplicationModel source) {
        super(source);
    }

    @Override
    public void customAfterPost(MetricsData postResult) {
        this.postResult = postResult;
    }

    public MetricsData getPostResult() {
        return postResult;
    }

    public static class MetricsData {

        public Long usedHeapMemory;

        public Long usedDirectMemory;

        public Long numHeapArenas;

        public Long numDirectArenas;

        public Long normalCacheSize;

        public Long smallCacheSize;

        public Long numThreadLocalCaches;

        public Long chunkSize;
    }
}
