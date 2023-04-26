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
package org.apache.dubbo.rpc.cluster.router.circuitBreaker.instance;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.metrics.aggregate.TimeWindowCounter;
import org.apache.dubbo.metrics.collector.AggregateMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * https://www.jianshu.com/p/e3408619718d
 * @param <T>
 */
public class CircuitBreakerInstanceImpl<T> implements CircuitBreakerInstance {

    /**
     * 关闭，打开，半打开
     */
    enum Status {
        CLOSED, OPEN, HALF_OPEN;
    }

    private final AtomicReference<Status> status = new AtomicReference<Status>(Status.CLOSED);

    private AtomicBoolean circuitOpen = new AtomicBoolean(false);

    private AtomicLong circuitOpenedOrLastTestedTime = new AtomicLong();

    private ScopeBeanFactory beanFactory = ApplicationModel.defaultModel().getBeanFactory();
    private AggregateMetricsCollector collector = beanFactory.getOrRegisterBean(AggregateMetricsCollector.class);

    private ConcurrentHashMap<MethodMetric, TimeWindowCounter> totalWindowCounterConcurrentHashMap = collector.getMethodTypeCounter().get(MetricsEvent.Type.TOTAL);

    private ConcurrentHashMap<MethodMetric, TimeWindowCounter> failedWindowCounterConcurrentHashMap = collector.getMethodTypeCounter().get(MetricsEvent.Type.TOTAL_FAILED);

    private final AtomicLong circuitOpened = new AtomicLong(-1);
    private Invoker<T> invoker;

    public Invoker<T> getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker<T> invoker) {
        this.invoker = invoker;
    }

    @Override
    public boolean allowRequest() {
//        if ((Boolean)this.properties.circuitBreakerForceOpen().get()) {
//            return false;
//        } else if ((Boolean)this.properties.circuitBreakerForceClosed().get()) {
//            this.isOpen();
//            return true;
//        } else {
//            return !this.isOpen() || this.allowSingleTest();
//        }
        return circuitOpen.get();
    }

    @Override
    public boolean isOpen() {
        if (this.circuitOpen.get()) {
            return true;
        }
        return false;
//        else {
//            HystrixCommandMetrics.HealthCounts health = this.metrics.getHealthCounts();
//            if (health.getTotalRequests() < (long) (Integer) this.properties.circuitBreakerRequestVolumeThreshold().get()) {
//                return false;
//            } else if (health.getErrorPercentage() < (Integer) this.properties.circuitBreakerErrorThresholdPercentage().get()) {
//                return false;
//            } else if (this.circuitOpen.compareAndSet(false, true)) {
//                this.circuitOpenedOrLastTestedTime.set(System.currentTimeMillis());
//                return true;
//            } else {
//                return true;
//            }
//        }
    }

    @Override
    public void markSuccess() {
        if (this.status.compareAndSet(CircuitBreakerInstanceImpl.Status.HALF_OPEN, CircuitBreakerInstanceImpl.Status.CLOSED)) {
            this.circuitOpened.set(-1L);
        }
    }

    @Override
    public void markNonSuccess() {
        if (this.status.compareAndSet(CircuitBreakerInstanceImpl.Status.HALF_OPEN, CircuitBreakerInstanceImpl.Status.OPEN)) {
            this.circuitOpened.set(System.currentTimeMillis());
        }
    }
}
