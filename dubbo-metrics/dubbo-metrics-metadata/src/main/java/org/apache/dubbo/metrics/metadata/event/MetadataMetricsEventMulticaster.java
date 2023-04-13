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

package org.apache.dubbo.metrics.metadata.event;

import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.metadata.type.ApplicationType;
import org.apache.dubbo.metrics.metadata.type.ServiceType;

import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_PUSH;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_STORE_PROVIDER_INTERFACE;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_SUBSCRIBE;

public final class MetadataMetricsEventMulticaster extends SimpleMetricsEventMulticaster {

    public MetadataMetricsEventMulticaster() {
        // MetricsPushListener
        super.addListener(onPostEventBuild(ApplicationType.P_TOTAL));
        super.addListener(onFinishEventBuild(ApplicationType.P_SUCCEED, OP_TYPE_PUSH.getType()));
        super.addListener(onErrorEventBuild(ApplicationType.P_FAILED, OP_TYPE_PUSH.getType()));

        // MetricsSubscribeListener
        super.addListener(onPostEventBuild(ApplicationType.S_TOTAL));
        super.addListener(onFinishEventBuild(ApplicationType.S_SUCCEED, OP_TYPE_SUBSCRIBE.getType()));
        super.addListener(onErrorEventBuild(ApplicationType.S_FAILED, OP_TYPE_SUBSCRIBE.getType()));

        // StoreProviderMetadataListener
        super.addListener(MetadataListener.onEvent(ServiceType.S_P_TOTAL,
            this::incrServiceKey
        ));
        super.addListener(MetadataListener.onFinish(ServiceType.S_P_SUCCEED,
            this::incrAndRt
        ));
        super.addListener(MetadataListener.onError(ServiceType.S_P_FAILED,
            this::incrAndRt
        ));

    }

    private void incrAndRt(MetadataEvent event, ServiceType type) {
        incrServiceKey(event, type);
        event.getCollector().addServiceKeyRT(event.getSource().getApplicationName(), event.getAttachmentValue(MetricsConstants.ATTACHMENT_KEY_SERVICE), OP_TYPE_STORE_PROVIDER_INTERFACE.getType(), event.getTimePair().calc());
    }

    private void incrServiceKey(MetadataEvent event, ServiceType type) {
        event.getCollector().incrementServiceKey(event.getSource().getApplicationName(), event.getAttachmentValue(MetricsConstants.ATTACHMENT_KEY_SERVICE), type, 1);
    }


    private MetadataListener onPostEventBuild(ApplicationType applicationType) {
        return MetadataListener.onEvent(applicationType,
            (event, type) -> event.getCollector().increment(event.getSource().getApplicationName(), type)
        );
    }

    private MetadataListener onFinishEventBuild(ApplicationType applicationType, String registryOpType) {
        return MetadataListener.onFinish(applicationType,
            (event, type) -> incrAndRt(event, applicationType, registryOpType)
        );
    }

    private MetadataListener onErrorEventBuild(ApplicationType applicationType, String registryOpType) {
        return MetadataListener.onError(applicationType,
            (event, type) -> incrAndRt(event, applicationType, registryOpType)
        );
    }

    private void incrAndRt(MetadataEvent event, ApplicationType applicationType, String registryOpType) {
        event.getCollector().increment(event.getSource().getApplicationName(), applicationType);
        event.getCollector().addApplicationRT(event.getSource().getApplicationName(), registryOpType, event.getTimePair().calc());
    }
}
