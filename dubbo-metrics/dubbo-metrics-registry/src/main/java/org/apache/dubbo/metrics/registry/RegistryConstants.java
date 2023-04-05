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

package org.apache.dubbo.metrics.registry;

import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.metrics.model.key.MetricsKeyDecorator;
import org.apache.dubbo.metrics.model.key.MetricsLevel;

import java.util.Arrays;
import java.util.List;

public interface RegistryConstants {

    String ATTACHMENT_KEY_SERVICE = "serviceKey";
    String ATTACHMENT_KEY_SIZE = "size";
    String ATTACHMENT_KEY_LAST_NUM_MAP = "lastNumMap";
    String ATTACHMENT_KEY_DIR_NUM = "dirNum";

    String OP_TYPE_REGISTER = "register";
    String OP_TYPE_SUBSCRIBE = "subscribe";
    String OP_TYPE_NOTIFY = "notify";
    String OP_TYPE_REGISTER_SERVICE = "register.service";
    String OP_TYPE_SUBSCRIBE_SERVICE = "subscribe.service";

    // App-level
    List<MetricsKey> appKeys = Arrays.asList(MetricsKey.REGISTER_METRIC_REQUESTS, MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED, MetricsKey.REGISTER_METRIC_REQUESTS_FAILED,
        MetricsKey.SUBSCRIBE_METRIC_NUM, MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED);
    MetricsKeyDecorator REGISTER_KEY = new MetricsKeyDecorator(MetricsLevel.APP, MetricsKey.REGISTER_METRIC_REQUESTS, MetricsKey.REGISTER_METRIC_REQUESTS_SUCCEED, MetricsKey.REGISTER_METRIC_REQUESTS_FAILED);
    MetricsKeyDecorator SUBSCRIBE_KEY = new MetricsKeyDecorator(MetricsLevel.APP, MetricsKey.SUBSCRIBE_METRIC_NUM, MetricsKey.SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsKey.SUBSCRIBE_METRIC_NUM_FAILED);
    MetricsKeyDecorator NOTIFY_KEY = new MetricsKeyDecorator(MetricsLevel.APP, MetricsKey.NOTIFY_METRIC_REQUESTS, MetricsKey.NOTIFY_METRIC_NUM_LAST, null);

    //Service-level
    List<MetricsKey> serviceKeys = Arrays.asList(MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS, MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED,
        MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM, MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED);
    MetricsKeyDecorator SERVICE_REGISTER_KEY = new MetricsKeyDecorator(MetricsLevel.SERVICE, MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS, MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_SUCCEED, MetricsKey.SERVICE_REGISTER_METRIC_REQUESTS_FAILED);
    MetricsKeyDecorator SERVICE_SUBSCRIBE_KEY = new MetricsKeyDecorator(MetricsLevel.SERVICE, MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM, MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_SUCCEED, MetricsKey.SERVICE_SUBSCRIBE_METRIC_NUM_FAILED);


}
