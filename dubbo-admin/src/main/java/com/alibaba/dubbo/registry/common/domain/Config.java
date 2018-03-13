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
package com.alibaba.dubbo.registry.common.domain;

/**
 * Config instance
 *
 */
public class Config extends Entity {

    public static final String MAIL_ENABLED = "MailEnabled";
    public static final String MAIL_HOST = "MailHost";
    public static final String MAIL_PORT = "MailPort";
    public static final String MAIL_FROM = "MailFrom";
    public static final String MAIL_AUTH = "MailAuth";
    public static final String MAIL_USERNAME = "MailUsername";
    public static final String MAIL_PASSWORD = "MailPassword";
    public static final String BULLETIN_MESSAGE = "BulletinMessage";
    public static final String ALLOW_ANONYMOUS_LOGIN = "AllowAnonymousLogin";  // Whether to allow anonymous login
    public static final String ALLOW_LEGACY_LOGIN = "AllowLegacyLogin";  // Whether to allow legacy system login
    public static final String MAX_THREAD_SIZE = "MaxThreadSize";  // The maximum number of threads
    public static final String MAX_CONNECTION_SIZE = "MaxConnectionSize";  // The maximum number of connections
    public static final String MAX_CACHE_SIZE = "MaxCacheSize";  // The maximum number of cache (not space)
    public static final String MAX_MAIL_SIZE = "MaxMailSize";  // The maximum number of mail queues
    public static final String ALIVED_CHECK_INTERVAL = "AlivedCheckInterval";
    public static final String DIRTY_CHECK_INTERVAL = "DirtyCheckInterval";
    public static final String CHANGED_CHECK_INTERVAL = "ChangedCheckInterval";
    public static final String CHANGED_CLEAR_INTERVAL = "ChangedClearInterval";
    public static final String FAILED_RETRY_INTERVAL = "FailedRetryInterval";
    public static final String HEARTBEAT_CHECK_INTERVAL = "HeartbeatCheckInterval";// Heartbeat check interval
    public static final String HEARTBEAT_CHECK_TIMEOUT = "HeartbeatCheckTimeout";// The biggest interval for not receive a heartbeat
    public static final String WARMUP_WAIT_TIME = "WarmupWaitTime";
    public static final String AUTO_REDIRECT_INTERVAL = "AutoRedirectInterval";
    public static final String AUTO_REDIRECT_THRESHOLD = "AutoRedirectThreshold";
    public static final String AUTO_REDIRECT_TOLERATE_PERCENT = "AutoRedirectToleratePercent";
    public static final String NOTIFY_TIMEOUT = "NotifyTimeout";
    public static final String ROUTE_ENABLED = "RouteEnabled";
    public static final String BUC_SERVICE_ADDRESS = "BucServiceAddress";
    public static final String DEFAULT_SERVICE_PARAMETERS = "DefaultServiceParameters";
    public static final String WARM_UP_ENABLED = "WarmupEnabled";
    public static final String HELP_DOCUMENT_URL = "HelpDocumentUrl";
    public static final String HOMEPAGE_DOMAIN = "HomepageDomain";
    public static final String HOMEPAGE_URL = "HomepageUrl";
    public static final String LOG_LEVEL = "LogLevel";
    public static final String DEFAULT_ROLE = "DefaultRole";  // The default role of user
    public static final String SERVER_ROUTE_ENABLED = "ServerRouteEnabled";
    private static final long serialVersionUID = 7938303018328907548L;
    private String key;

    private String value;

    private String username;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @java.lang.Override
    public String toString() {
        return key + "=" + value;
    }

}
