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
package org.apache.dubbo.monitor.logstash;

import org.apache.dubbo.common.URL;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;

public class MonitorData {
    @JSONField(serialize = false)
    public long timestamp;
    @JSONField(serialize = false)
    public URL url;
    @JSONField(name = "@timestamp", format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    public Date date;

    public String application;
    public String group;
    public String client;
    public String server;
    public String method;
    public String service;
    public String version;

    @JSONField(name = "metric.success")
    public long success;
    @JSONField(name = "metric.failure")
    public long failure;
    @JSONField(name = "metric.input")
    public long input;
    @JSONField(name = "metric.output")
    public long output;
    @JSONField(name = "metric.elapsed")
    public long elapsed;
    @JSONField(name = "metric.concurrent")
    public long concurrent;
    @JSONField(name = "metric.maxInput")
    public long maxInput;
    @JSONField(name = "metric.maxOutput")
    public long maxOutput;
    @JSONField(name = "metric.maxElapsed")
    public long maxElapsed;
    @JSONField(name = "metric.maxConcurrent")
    public long maxConcurrent;
}
