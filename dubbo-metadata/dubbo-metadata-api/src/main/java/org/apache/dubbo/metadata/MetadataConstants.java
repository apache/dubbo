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
package org.apache.dubbo.metadata;

public class MetadataConstants {
    public static final String KEY_SEPARATOR = ":";
    public static final String DEFAULT_PATH_TAG = "metadata";
    public static final String KEY_REVISON_PREFIX = "revision";
    public static final String META_DATA_STORE_TAG = ".metaData";
    public static final String SERVICE_META_DATA_STORE_TAG = ".smd";
    public static final String CONSUMER_META_DATA_STORE_TAG = ".cmd";
    public static final String METADATA_PUBLISH_DELAY_KEY = "dubbo.application.metadata.publish.delay";
    public static final int DEFAULT_METADATA_PUBLISH_DELAY = 30000;
    public static final String METADATA_PROXY_TIMEOUT_KEY = "dubbo.application.metadata.proxy.delay";
    public static final int DEFAULT_METADATA_TIMEOUT_VALUE = 5000;
}
