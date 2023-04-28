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

package org.apache.dubbo.metrics.model;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.metrics.collector.MethodMetricsCollector;
import org.apache.dubbo.metrics.collector.ServiceMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;
import static org.apache.dubbo.metrics.MetricsConstants.ATTACHMENT_KEY_SERVICE;
import static org.apache.dubbo.metrics.MetricsConstants.INVOCATION;
import static org.apache.dubbo.metrics.MetricsConstants.SELF_INCREMENT_SIZE;

public class MetricsSupport {

    private static final String version = Version.getVersion();
    private static final String commitId = Version.getLastCommitId();

    public static Map<String, String> applicationTags(String applicationName) {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);
        tags.put(TAG_APPLICATION_VERSION_KEY, version);
        tags.put(MetricsKey.METADATA_GIT_COMMITID_METRIC.getName(), commitId);
        return tags;
    }

    public static Map<String, String> serviceTags(String appAndServiceName) {
        String[] keys = appAndServiceName.split("_");
        if (keys.length != 2) {
            throw new MetricsNeverHappenException("Error service name: " + appAndServiceName);
        }
        Map<String, String> tags = applicationTags(keys[0]);
        tags.put(TAG_INTERFACE_KEY, keys[1]);
        return tags;
    }

    public static Map<String, String> methodTags(String names) {
        String[] keys = names.split("_");
        if (keys.length != 3) {
            throw new MetricsNeverHappenException("Error names: " + names);
        }
        Map<String, String> tags = applicationTags(keys[0]);
        tags.put(TAG_INTERFACE_KEY, keys[1]);
        tags.put(TAG_METHOD_KEY, keys[2]);
        return tags;
    }

    public static MetricsKey getMetricsKey(RpcException e) {
        MetricsKey targetKey;
        targetKey = MetricsKey.METRIC_REQUESTS_FAILED;
        if (e.isTimeout()) {
            targetKey = MetricsKey.METRIC_REQUESTS_TIMEOUT;
        }
        if (e.isLimitExceed()) {
            targetKey = MetricsKey.METRIC_REQUESTS_LIMIT;
        }
        if (e.isBiz()) {
            targetKey = MetricsKey.METRIC_REQUEST_BUSINESS_FAILED;
        }
        if (e.isSerialization()) {
            targetKey = MetricsKey.METRIC_REQUESTS_CODEC_FAILED;
        }
        if (e.isNetwork()) {
            targetKey = MetricsKey.METRIC_REQUESTS_NETWORK_FAILED;
        }
        return targetKey;
    }

    public static MetricsKey getAggMetricsKey(RpcException e) {
        MetricsKey targetKey;
        targetKey = MetricsKey.METRIC_REQUESTS_FAILED_AGG;
        if (e.isTimeout()) {
            targetKey = MetricsKey.METRIC_REQUESTS_TIMEOUT_AGG;
        }
        if (e.isLimitExceed()) {
            targetKey = MetricsKey.METRIC_REQUESTS_LIMIT_AGG;
        }
        if (e.isBiz()) {
            targetKey = MetricsKey.METRIC_REQUEST_BUSINESS_FAILED_AGG;
        }
        if (e.isSerialization()) {
            targetKey = MetricsKey.METRIC_REQUESTS_CODEC_FAILED_AGG;
        }
        if (e.isNetwork()) {
            targetKey = MetricsKey.METRIC_REQUESTS_NETWORK_FAILED_AGG;
        }
        return targetKey;
    }

    public static String getSide(Invocation invocation) {
        Optional<? extends Invoker<?>> invoker = Optional.ofNullable(invocation.getInvoker());
        return invoker.isPresent() ? invoker.get().getUrl().getSide() : PROVIDER_SIDE;
    }


    public static String getInterfaceName(Invocation invocation) {
        String serviceUniqueName = invocation.getTargetServiceUniqueName();
        String interfaceAndVersion;
        String[] arr = serviceUniqueName.split(PATH_SEPARATOR);
        if (arr.length == 2) {
            interfaceAndVersion = arr[1];
        } else {
            interfaceAndVersion = arr[0];
        }
        String[] ivArr = interfaceAndVersion.split(GROUP_CHAR_SEPARATOR);
        return ivArr[0];
    }

    /**
     * Incr service num
     */
    public static void increment(MetricsKey metricsKey, MetricsPlaceValue placeType, ServiceMetricsCollector<TimeCounterEvent> collector, MetricsEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), new MetricsKeyWrapper(metricsKey, placeType), SELF_INCREMENT_SIZE);
    }

    /**
     * Dec service num
     */
    public static void dec(MetricsKey metricsKey, MetricsPlaceValue placeType, ServiceMetricsCollector<TimeCounterEvent> collector, MetricsEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), new MetricsKeyWrapper(metricsKey, placeType), -SELF_INCREMENT_SIZE);
    }

    /**
     * Incr service num&&rt
     */
    public static void incrAndAddRt(MetricsKey metricsKey, MetricsPlaceValue placeType, ServiceMetricsCollector<TimeCounterEvent> collector, TimeCounterEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), new MetricsKeyWrapper(metricsKey, placeType), SELF_INCREMENT_SIZE);
        collector.addRt(event.appName(), event.getAttachmentValue(ATTACHMENT_KEY_SERVICE), placeType.getType(), event.getTimePair().calc());
    }

    /**
     * Incr method num
     */
    public static void increment(MetricsKey metricsKey, MetricsPlaceValue placeType, MethodMetricsCollector<TimeCounterEvent> collector, MetricsEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(INVOCATION), new MetricsKeyWrapper(metricsKey, placeType), SELF_INCREMENT_SIZE);
    }

    /**
     * Dec method num
     */
    public static void dec(MetricsKey metricsKey, MetricsPlaceValue placeType, MethodMetricsCollector<TimeCounterEvent> collector, MetricsEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(INVOCATION), new MetricsKeyWrapper(metricsKey, placeType), -SELF_INCREMENT_SIZE);
    }

    /**
     * Incr method num&&rt
     */
    public static void incrAndAddRt(MetricsKey metricsKey, MetricsPlaceValue placeType, MethodMetricsCollector<TimeCounterEvent> collector, TimeCounterEvent event) {
        collector.increment(event.appName(), event.getAttachmentValue(INVOCATION), new MetricsKeyWrapper(metricsKey, placeType), SELF_INCREMENT_SIZE);
        collector.addRt(event.appName(), event.getAttachmentValue(INVOCATION), placeType.getType(), event.getTimePair().calc());
    }
}
