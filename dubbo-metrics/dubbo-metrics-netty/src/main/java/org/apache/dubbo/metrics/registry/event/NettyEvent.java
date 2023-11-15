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
package org.apache.dubbo.metrics.registry.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.metrics.registry.collector.NettyMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.MetricsConstants.NETTY_METRICS_MAP;

/**
 * Netty related events
 */
public class NettyEvent extends TimeCounterEvent {
    public NettyEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel, typeWrapper);
        ScopeBeanFactory beanFactory = getSource().getBeanFactory();
        NettyMetricsCollector collector;
        if (!beanFactory.isDestroyed()) {
            collector = beanFactory.getBean(NettyMetricsCollector.class);
            super.setAvailable(collector != null && collector.isCollectEnabled());
        }
    }

    public static NettyEvent toNettyEvent(ApplicationModel applicationModel) {
        return new NettyEvent(applicationModel, new TypeWrapper(MetricsLevel.APP, null, null, null)) {
            @Override
            public void customAfterPost(Object postResult) {
                super.putAttachment(NETTY_METRICS_MAP, postResult);
            }
        };
    }
}
