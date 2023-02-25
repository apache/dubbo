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

package org.apache.dubbo.metrics.model.container;

import org.apache.dubbo.metrics.model.MetricsKey;
import org.apache.dubbo.metrics.model.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.sample.GaugeMetricSample;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Long type data container
 * @param <NUMBER>
 */
public class LongContainer<NUMBER extends Number> extends ConcurrentHashMap<String, NUMBER> {

    /**
     * Provide the metric type name
     */
    private final MetricsKeyWrapper metricsKeyWrapper;
    /**
     * The initial value corresponding to the key is generally 0 of different data types
     */
    private final Function<String, NUMBER> initFunc;
    /**
     * Statistical data calculation function, which can be self-increment, self-decrement, or more complex avg function
     */
    private final BiConsumer<Long, NUMBER> consumerFunc;
    /**
     * Data output function required by  {@link GaugeMetricSample GaugeMetricSample}
     */
    private Function<String, Long> valueSupplier;


    public LongContainer(MetricsKeyWrapper metricsKeyWrapper, Supplier<NUMBER> initFunc, BiConsumer<Long, NUMBER> consumerFunc) {
        this.metricsKeyWrapper = metricsKeyWrapper;
        this.initFunc = s -> initFunc.get();
        this.consumerFunc = consumerFunc;
        this.valueSupplier = k -> this.get(k).longValue();
    }

    public boolean specifyType(String type) {
        return type.equals(getMetricsKeyWrapper().getType());
    }

    public MetricsKeyWrapper getMetricsKeyWrapper() {
        return metricsKeyWrapper;
    }

    public boolean isKeyWrapper(MetricsKey metricsKey, String registryOpType) {
        return metricsKeyWrapper.isKey(metricsKey, registryOpType);
    }

    public Function<String, NUMBER> getInitFunc() {
        return initFunc;
    }

    public BiConsumer<Long, NUMBER> getConsumerFunc() {
        return consumerFunc;
    }

    public Function<String, Long> getValueSupplier() {
        return valueSupplier;
    }

    public void setValueSupplier(Function<String, Long> valueSupplier) {
        this.valueSupplier = valueSupplier;
    }

    @Override
    public String toString() {
        return "LongContainer{" +
            "metricsKeyWrapper=" + metricsKeyWrapper +
            '}';
    }
}
