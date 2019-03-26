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
package org.apache.dubbo.metrics;

import java.lang.reflect.Method;

/**
 * The design concept is heavily borrowed from SLF4j (http://www.slf4j.org/), the logging framework.
 * The application only depends on the metrics api.
 * The implementation will be dynamically bound.
 * If the implementation if not found in classpath, by default the {@link NOPMetricManager} will be bound.
 */
public class MetricManager {

    private static final String BINDER_CLASS = "org.apache.dubbo.metrics.MetricManagerBinder";

    private static final IMetricManager NOP_METRIC_MANAGER = new NOPMetricManager();

    private static volatile IMetricManager iMetricManager;

    /**
     * Create a {@link Counter} metric in given group, and name.
     * if not exist, an instance will be created.
     *
     * @param group the group of MetricRegistry
     * @param name the name of the metric
     * @return an instance of counter
     */
    public static Counter getCounter(String group, MetricName name) {
        IMetricManager manager = getIMetricManager();
        return manager.getCounter(group, name);
    }

    /**
     * Create a {@link BucketCounter} metric in given group, and name.
     * if not exist, an instance will be created.
     *
     * @param group the group of MetricRegistry
     * @param name the name of the metric
     * @return an instance of {@link BucketCounter}
     */
    public static BucketCounter getBucketCounters(String group, MetricName name) {
        IMetricManager manager = getIMetricManager();
        return manager.getBucketCounter(group, name);
    }

    /**
     * Create a {@link Compass} metric in given group, and name
     * if not exist, an instance will be created.
     *
     * @param group the group of MetricRegistry
     * @param name the name of the metric
     * @return an instance of {@link Compass}
     */
    public static Compass getCompass(String group, MetricName name) {
        IMetricManager manager = getIMetricManager();
        return manager.getCompass(group, name);
    }

    /**
     * Register a customized metric to specified group.
     * @param group the group name of MetricRegistry
     * @param metric the metric to register
     */
    public static void register(String group, MetricName name, Metric metric) {
        IMetricManager manager = getIMetricManager();
        manager.register(group, name, metric);
    }

    /**
     * get dynamically bound {@link IMetricManager} instance
     * @return the {@link IMetricManager} instance bound
     */
    @SuppressWarnings("unchecked")
    public static IMetricManager getIMetricManager() {
        if (iMetricManager == null) {
            synchronized (MetricManager.class) {
                if (iMetricManager == null) {
                    try {
                        Class binderClazz = MetricManager.class.getClassLoader().loadClass(BINDER_CLASS);
                        Method getSingleton = binderClazz.getMethod("getSingleton");
                        Object binderObject = getSingleton.invoke(null);
                        Method getMetricManager = binderClazz.getMethod("getMetricManager");
                        iMetricManager = (IMetricManager) getMetricManager.invoke(binderObject);
                    } catch (Exception e) {
                        iMetricManager = NOP_METRIC_MANAGER;
                    }
                }
            }
        }
        return iMetricManager;
    }

}
