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
package org.apache.dubbo.metrics.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.MetricsConstants;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.MetricsSupport;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.metrics.model.key.TypeWrapper;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.metrics.DefaultConstants.METRIC_THROWABLE;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUESTS_SUCCEED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.METRIC_REQUEST_BUSINESS_FAILED;

/**
 * Request related events
 */
public class RequestEvent extends TimeCounterEvent {
    private static final TypeWrapper REQUEST_EVENT = new TypeWrapper(
            MetricsLevel.SERVICE, METRIC_REQUESTS, METRIC_REQUESTS_SUCCEED, METRIC_REQUEST_BUSINESS_FAILED);
    private static final TypeWrapper REQUEST_ERROR_EVENT =
            new TypeWrapper(MetricsLevel.METHOD, MetricsKey.METRIC_REQUESTS);

    public RequestEvent(
            ApplicationModel applicationModel,
            String appName,
            MetricsDispatcher metricsDispatcher,
            DefaultMetricsCollector collector,
            TypeWrapper TYPE_WRAPPER) {
        super(applicationModel, appName, metricsDispatcher, TYPE_WRAPPER);
        if (collector == null) {
            ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
            if (!beanFactory.isDestroyed()) {
                collector = beanFactory.getBean(DefaultMetricsCollector.class);
            }
        }
        super.setAvailable(collector != null && collector.isCollectEnabled());
    }

    public static RequestEvent toRequestEvent(
            ApplicationModel applicationModel,
            String appName,
            MetricsDispatcher metricsDispatcher,
            DefaultMetricsCollector collector,
            Invocation invocation,
            String side,
            boolean serviceLevel) {
        MethodMetric methodMetric = new MethodMetric(applicationModel, invocation, serviceLevel);
        RequestEvent requestEvent =
                new RequestEvent(applicationModel, appName, metricsDispatcher, collector, REQUEST_EVENT);
        requestEvent.putAttachment(MetricsConstants.INVOCATION, invocation);
        requestEvent.putAttachment(MetricsConstants.METHOD_METRICS, methodMetric);
        requestEvent.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
        requestEvent.putAttachment(MetricsConstants.INVOCATION_SIDE, side);
        return requestEvent;
    }

    @Override
    public void customAfterPost(Object postResult) {
        if (postResult == null) {
            return;
        }
        if (!(postResult instanceof Result)) {
            throw new MetricsNeverHappenException(
                    "Result type error, postResult:" + postResult.getClass().getName());
        }
        super.putAttachment(METRIC_THROWABLE, ((Result) postResult).getException());
    }

    /**
     * Acts on MetricsClusterFilter to monitor exceptions that occur before request execution
     */
    public static RequestEvent toRequestErrorEvent(
            ApplicationModel applicationModel,
            String appName,
            MetricsDispatcher metricsDispatcher,
            Invocation invocation,
            String side,
            int code,
            boolean serviceLevel) {
        RequestEvent event = new RequestEvent(applicationModel, appName, metricsDispatcher, null, REQUEST_ERROR_EVENT);
        event.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
        event.putAttachment(MetricsConstants.INVOCATION_SIDE, side);
        event.putAttachment(MetricsConstants.INVOCATION, invocation);
        event.putAttachment(MetricsConstants.INVOCATION_REQUEST_ERROR, code);
        event.putAttachment(
                MetricsConstants.METHOD_METRICS, new MethodMetric(applicationModel, invocation, serviceLevel));
        return event;
    }

    public boolean isRequestErrorEvent() {
        return super.getAttachmentValue(MetricsConstants.INVOCATION_REQUEST_ERROR) != null;
    }
}
