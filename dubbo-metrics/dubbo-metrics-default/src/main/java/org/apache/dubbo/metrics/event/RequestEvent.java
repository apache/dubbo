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
    public RequestEvent(ApplicationModel applicationModel, TypeWrapper typeWrapper) {
        super(applicationModel,typeWrapper);
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        DefaultMetricsCollector collector;
        if (!beanFactory.isDestroyed()) {
            collector = beanFactory.getBean(DefaultMetricsCollector.class);
            super.setAvailable(collector != null && collector.isCollectEnabled());
        }
    }

    public static RequestEvent toRequestEvent(ApplicationModel applicationModel, Invocation invocation) {
        RequestEvent requestEvent = new RequestEvent(applicationModel, new TypeWrapper(MetricsLevel.SERVICE, METRIC_REQUESTS, METRIC_REQUESTS_SUCCEED, METRIC_REQUEST_BUSINESS_FAILED)) {
            @Override
            public void customAfterPost(Object postResult) {
                if (postResult == null) {
                    return;
                }
                if (!(postResult instanceof Result)) {
                    throw new MetricsNeverHappenException("Result type error, postResult:" + postResult.getClass().getName());
                }
                super.putAttachment(METRIC_THROWABLE, ((Result) postResult).getException());
            }
        };
        requestEvent.putAttachment(MetricsConstants.INVOCATION, invocation);
        requestEvent.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
        requestEvent.putAttachment(MetricsConstants.INVOCATION_SIDE, MetricsSupport.getSide(invocation));
        return requestEvent;
    }

    /**
     * Acts on MetricsClusterFilter to monitor exceptions that occur before request execution
     */
    public static RequestEvent toRequestBeforeEvent(ApplicationModel applicationModel, Invocation invocation) {
        RequestEvent event = new RequestEvent(applicationModel, new TypeWrapper(MetricsLevel.METHOD, MetricsKey.METRIC_REQUESTS));
        event.putAttachment(ATTACHMENT_KEY_SERVICE, MetricsSupport.getInterfaceName(invocation));
        event.putAttachment(MetricsConstants.INVOCATION_SIDE, MetricsSupport.getSide(invocation));
        event.putAttachment(MetricsConstants.INVOCATION, invocation);
        event.putAttachment(MetricsConstants.INVOCATION_REQUEST_BEFORE, "");
        return event;
    }

    public boolean isRequestBeforeEvent(){
        return super.getAttachmentValue(MetricsConstants.INVOCATION_REQUEST_BEFORE) != null;
    }
}
