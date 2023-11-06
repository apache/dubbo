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
import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metrics.collector.MethodMetricsCollector;
import org.apache.dubbo.metrics.collector.ServiceMetricsCollector;
import org.apache.dubbo.metrics.event.MetricsEvent;
import org.apache.dubbo.metrics.event.TimeCounterEvent;
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyWrapper;
import org.apache.dubbo.metrics.model.key.MetricsPlaceValue;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_CHAR_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_MODULE;
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
import static org.apache.dubbo.metrics.MetricsConstants.METHOD_METRICS;
import static org.apache.dubbo.metrics.MetricsConstants.SELF_INCREMENT_SIZE;

public class MetricsSupport {

    private static final String version = Version.getVersion();

    private static final String commitId = Version.getLastCommitId();

    public static Map<String, String> applicationTags(ApplicationModel applicationModel) {
        return applicationTags(applicationModel, null);
    }

    public static Map<String, String> applicationTags(
            ApplicationModel applicationModel, @Nullable Map<String, String> extraInfo) {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_APPLICATION_NAME, applicationModel.getApplicationName());
        tags.put(TAG_APPLICATION_MODULE, applicationModel.getInternalId());
        if (CollectionUtils.isNotEmptyMap(extraInfo)) {
            tags.putAll(extraInfo);
        }
        return tags;
    }

    public static Map<String, String> gitTags(Map<String, String> tags) {
        tags.put(MetricsKey.METADATA_GIT_COMMITID_METRIC.getName(), commitId);
        tags.put(TAG_APPLICATION_VERSION_KEY, version);
        return tags;
    }

    public static Map<String, String> hostTags(Map<String, String> tags) {
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        return tags;
    }

    public static Map<String, String> serviceTags(
            ApplicationModel applicationModel, String serviceKey, Map<String, String> extraInfo) {
        Map<String, String> tags = applicationTags(applicationModel, extraInfo);
        tags.put(TAG_INTERFACE_KEY, serviceKey);
        return tags;
    }

    public static Map<String, String> methodTags(
            ApplicationModel applicationModel, String serviceKey, String methodName) {
        Map<String, String> tags = applicationTags(applicationModel);
        tags.put(TAG_INTERFACE_KEY, serviceKey);
        tags.put(TAG_METHOD_KEY, methodName);
        return tags;
    }

    public static MetricsKey getMetricsKey(Throwable throwable) {
        MetricsKey targetKey = MetricsKey.METRIC_REQUESTS_FAILED;
        if (throwable instanceof RpcException) {
            RpcException e = (RpcException) throwable;
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
        } else {
            targetKey = MetricsKey.METRIC_REQUEST_BUSINESS_FAILED;
        }
        return targetKey;
    }

    public static MetricsKey getAggMetricsKey(Throwable throwable) {
        MetricsKey targetKey = MetricsKey.METRIC_REQUESTS_FAILED_AGG;
        if (throwable instanceof RpcException) {
            RpcException e = (RpcException) throwable;
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
        } else {
            targetKey = MetricsKey.METRIC_REQUEST_BUSINESS_FAILED_AGG;
        }
        return targetKey;
    }

    public static String getSide(Invocation invocation) {
        Optional<? extends Invoker<?>> invoker = Optional.ofNullable(invocation.getInvoker());
        return invoker.isPresent() ? invoker.get().getUrl().getSide() : PROVIDER_SIDE;
    }

    public static String getInterfaceName(Invocation invocation) {
        if (invocation.getServiceModel() != null && invocation.getServiceModel().getServiceMetadata() != null) {
            return invocation.getServiceModel().getServiceMetadata().getServiceInterfaceName();
        } else {
            String serviceUniqueName = invocation.getTargetServiceUniqueName();
            String interfaceAndVersion;
            if (StringUtils.isBlank(serviceUniqueName)) {
                return "";
            }
            String[] arr = serviceUniqueName.split(PATH_SEPARATOR);
            if (arr.length == 2) {
                interfaceAndVersion = arr[1];
            } else {
                interfaceAndVersion = arr[0];
            }
            String[] ivArr = interfaceAndVersion.split(GROUP_CHAR_SEPARATOR);
            return ivArr[0];
        }
    }

    public static String getGroup(Invocation invocation) {
        if (invocation.getServiceModel() != null && invocation.getServiceModel().getServiceMetadata() != null) {
            return invocation.getServiceModel().getServiceMetadata().getGroup();
        } else {
            String serviceUniqueName = invocation.getTargetServiceUniqueName();
            String group = null;
            String[] arr = serviceUniqueName.split(PATH_SEPARATOR);
            if (arr.length == 2) {
                group = arr[0];
            }
            return group;
        }
    }

    public static String getVersion(Invocation invocation) {
        if (invocation.getServiceModel() != null && invocation.getServiceModel().getServiceMetadata() != null) {
            return invocation.getServiceModel().getServiceMetadata().getVersion();
        } else {
            String interfaceAndVersion;
            String[] arr = invocation.getTargetServiceUniqueName().split(PATH_SEPARATOR);
            if (arr.length == 2) {
                interfaceAndVersion = arr[1];
            } else {
                interfaceAndVersion = arr[0];
            }
            String[] ivArr = interfaceAndVersion.split(GROUP_CHAR_SEPARATOR);
            return ivArr.length == 2 ? ivArr[1] : null;
        }
    }

    /**
     * Incr service num
     */
    public static void increment(
            MetricsKey metricsKey,
            MetricsPlaceValue placeType,
            ServiceMetricsCollector<TimeCounterEvent> collector,
            MetricsEvent event) {
        collector.increment(
                event.getAttachmentValue(ATTACHMENT_KEY_SERVICE),
                new MetricsKeyWrapper(metricsKey, placeType),
                SELF_INCREMENT_SIZE);
    }

    /**
     * Incr service num&&rt
     */
    public static void incrAndAddRt(
            MetricsKey metricsKey,
            MetricsPlaceValue placeType,
            ServiceMetricsCollector<TimeCounterEvent> collector,
            TimeCounterEvent event) {
        collector.increment(
                event.getAttachmentValue(ATTACHMENT_KEY_SERVICE),
                new MetricsKeyWrapper(metricsKey, placeType),
                SELF_INCREMENT_SIZE);
        Invocation invocation = event.getAttachmentValue(INVOCATION);
        if (invocation != null) {
            collector.addServiceRt(
                    invocation, placeType.getType(), event.getTimePair().calc());
        } else {
            collector.addServiceRt(
                    (String) event.getAttachmentValue(ATTACHMENT_KEY_SERVICE),
                    placeType.getType(),
                    event.getTimePair().calc());
        }
    }

    /**
     * Incr method num
     */
    public static void increment(
            MetricsKey metricsKey,
            MetricsPlaceValue placeType,
            MethodMetricsCollector<TimeCounterEvent> collector,
            MetricsEvent event) {
        collector.increment(
                event.getAttachmentValue(METHOD_METRICS),
                new MetricsKeyWrapper(metricsKey, placeType),
                SELF_INCREMENT_SIZE);
    }

    public static void init(
            MetricsKey metricsKey,
            MetricsPlaceValue placeType,
            MethodMetricsCollector<TimeCounterEvent> collector,
            MetricsEvent event) {
        collector.init(event.getAttachmentValue(INVOCATION), new MetricsKeyWrapper(metricsKey, placeType));
    }

    /**
     * Dec method num
     */
    public static void dec(
            MetricsKey metricsKey,
            MetricsPlaceValue placeType,
            MethodMetricsCollector<TimeCounterEvent> collector,
            MetricsEvent event) {
        collector.increment(
                event.getAttachmentValue(METHOD_METRICS),
                new MetricsKeyWrapper(metricsKey, placeType),
                -SELF_INCREMENT_SIZE);
    }

    /**
     * Incr method num&&rt
     */
    public static void incrAndAddRt(
            MetricsKey metricsKey,
            MetricsPlaceValue placeType,
            MethodMetricsCollector<TimeCounterEvent> collector,
            TimeCounterEvent event) {
        collector.increment(
                event.getAttachmentValue(METHOD_METRICS),
                new MetricsKeyWrapper(metricsKey, placeType),
                SELF_INCREMENT_SIZE);
        collector.addMethodRt(
                event.getAttachmentValue(INVOCATION),
                placeType.getType(),
                event.getTimePair().calc());
    }

    /**
     * Generate a complete indicator item for an interface/method
     */
    public static <T> void fillZero(Map<?, Map<T, AtomicLong>> data) {
        if (CollectionUtils.isEmptyMap(data)) {
            return;
        }
        Set<T> allKeyMetrics =
                data.values().stream().flatMap(map -> map.keySet().stream()).collect(Collectors.toSet());
        data.forEach((keyWrapper, mapVal) -> {
            for (T key : allKeyMetrics) {
                mapVal.computeIfAbsent(key, k -> new AtomicLong(0));
            }
        });
    }
}
