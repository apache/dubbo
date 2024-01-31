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

import org.apache.dubbo.metrics.event.SimpleMetricsEventMulticaster;
import org.apache.dubbo.metrics.listener.MetricsApplicationListener;
import org.apache.dubbo.metrics.listener.MetricsServiceListener;
import org.apache.dubbo.metrics.metadata.collector.MetadataMetricsCollector;
import org.apache.dubbo.metrics.model.key.CategoryOverall;
import org.apache.dubbo.metrics.model.key.MetricsCat;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_PUSH;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_STORE_PROVIDER_INTERFACE;
import static org.apache.dubbo.metrics.metadata.MetadataMetricsConstants.OP_TYPE_SUBSCRIBE;

public final class MetadataSubDispatcher extends SimpleMetricsEventMulticaster {

    public MetadataSubDispatcher(MetadataMetricsCollector collector) {

        CategorySet.ALL.forEach(categorySet -> {
            super.addListener(categorySet.getPost().getEventFunc().apply(collector));
            if (categorySet.getFinish() != null) {
                super.addListener(categorySet.getFinish().getEventFunc().apply(collector));
            }
            if (categorySet.getError() != null) {
                super.addListener(categorySet.getError().getEventFunc().apply(collector));
            }
        });
    }

    /**
     * A closer aggregation of MetricsCat, a summary collection of certain types of events
     */
    interface CategorySet {
        CategoryOverall APPLICATION_PUSH = new CategoryOverall(
                OP_TYPE_PUSH, MCat.APPLICATION_PUSH_POST, MCat.APPLICATION_PUSH_FINISH, MCat.APPLICATION_PUSH_ERROR);
        CategoryOverall APPLICATION_SUBSCRIBE = new CategoryOverall(
                OP_TYPE_SUBSCRIBE,
                MCat.APPLICATION_SUBSCRIBE_POST,
                MCat.APPLICATION_SUBSCRIBE_FINISH,
                MCat.APPLICATION_SUBSCRIBE_ERROR);
        CategoryOverall SERVICE_SUBSCRIBE = new CategoryOverall(
                OP_TYPE_STORE_PROVIDER_INTERFACE,
                MCat.SERVICE_SUBSCRIBE_POST,
                MCat.SERVICE_SUBSCRIBE_FINISH,
                MCat.SERVICE_SUBSCRIBE_ERROR);

        List<CategoryOverall> ALL = Arrays.asList(APPLICATION_PUSH, APPLICATION_SUBSCRIBE, SERVICE_SUBSCRIBE);
    }

    /**
     *  {@link MetricsCat} MetricsCat collection, for better classification processing
     *  Except for a few custom functions, most of them can build standard event listening functions through the static methods of MetricsApplicationListener
     */
    interface MCat {
        // MetricsPushListener
        MetricsCat APPLICATION_PUSH_POST =
                new MetricsCat(MetricsKey.METADATA_PUSH_METRIC_NUM, MetricsApplicationListener::onPostEventBuild);
        MetricsCat APPLICATION_PUSH_FINISH = new MetricsCat(
                MetricsKey.METADATA_PUSH_METRIC_NUM_SUCCEED, MetricsApplicationListener::onFinishEventBuild);
        MetricsCat APPLICATION_PUSH_ERROR = new MetricsCat(
                MetricsKey.METADATA_PUSH_METRIC_NUM_FAILED, MetricsApplicationListener::onErrorEventBuild);

        // MetricsSubscribeListener
        MetricsCat APPLICATION_SUBSCRIBE_POST =
                new MetricsCat(MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM, MetricsApplicationListener::onPostEventBuild);
        MetricsCat APPLICATION_SUBSCRIBE_FINISH = new MetricsCat(
                MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsApplicationListener::onFinishEventBuild);
        MetricsCat APPLICATION_SUBSCRIBE_ERROR = new MetricsCat(
                MetricsKey.METADATA_SUBSCRIBE_METRIC_NUM_FAILED, MetricsApplicationListener::onErrorEventBuild);

        // MetricsSubscribeListener
        MetricsCat SERVICE_SUBSCRIBE_POST =
                new MetricsCat(MetricsKey.STORE_PROVIDER_METADATA, MetricsServiceListener::onPostEventBuild);
        MetricsCat SERVICE_SUBSCRIBE_FINISH =
                new MetricsCat(MetricsKey.STORE_PROVIDER_METADATA_SUCCEED, MetricsServiceListener::onFinishEventBuild);
        MetricsCat SERVICE_SUBSCRIBE_ERROR =
                new MetricsCat(MetricsKey.STORE_PROVIDER_METADATA_FAILED, MetricsServiceListener::onErrorEventBuild);
    }
}
