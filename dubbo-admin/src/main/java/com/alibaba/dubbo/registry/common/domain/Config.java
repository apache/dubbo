/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-6-30
 * $Id: Config.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.domain;

/**
 * 配置对象
 * @author rain.chenjr
 *
 */
public class Config extends Entity{

    private static final long serialVersionUID = 7938303018328907548L;
    
    public static final String MAIL_ENABLED = "MailEnabled";  //是否允许匿名登录

    public static final String MAIL_HOST = "MailHost";  //是否允许匿名登录

    public static final String MAIL_PORT = "MailPort";  //是否允许匿名登录

    public static final String MAIL_FROM = "MailFrom";  //是否允许匿名登录

    public static final String MAIL_AUTH = "MailAuth";  //是否允许匿名登录

    public static final String MAIL_USERNAME = "MailUsername";  //是否允许匿名登录

    public static final String MAIL_PASSWORD = "MailPassword";  //是否允许匿名登录

    public static final String BULLETIN_MESSAGE = "BulletinMessage";  //是否允许匿名登录

    public static final String ALLOW_ANONYMOUS_LOGIN = "AllowAnonymousLogin";  //是否允许匿名登录

    public static final String ALLOW_LEGACY_LOGIN = "AllowLegacyLogin";  //是否允许遗留系统登录
    
    public static final String MAX_THREAD_SIZE = "MaxThreadSize";  // 最大线程数

    public static final String MAX_CONNECTION_SIZE = "MaxConnectionSize";  // 最大连接数

    public static final String MAX_CACHE_SIZE = "MaxCacheSize";  // 最大缓存数
    
    public static final String MAX_MAIL_SIZE = "MaxMailSize";  // 最大邮件队列数
    
    public static final String ALIVED_CHECK_INTERVAL = "AlivedCheckInterval";

    public static final String DIRTY_CHECK_INTERVAL = "DirtyCheckInterval";
    
    public static final String CHANGED_CHECK_INTERVAL = "ChangedCheckInterval";
    
    public static final String CHANGED_CLEAR_INTERVAL = "ChangedClearInterval";
    
    public static final String FAILED_RETRY_INTERVAL = "FailedRetryInterval";

    public static final String HEARTBEAT_CHECK_INTERVAL = "HeartbeatCheckInterval";//心跳检查间隔
    
    public static final String HEARTBEAT_CHECK_TIMEOUT = "HeartbeatCheckTimeout";//心跳超时时间

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
    
    public static final String LOG_LEVEL = "LogLevel";  // 日志级别
    
    public static final String DEFAULT_ROLE= "DefaultRole";  // 默认创建用户的权限
    
    public static final String SERVER_ROUTE_ENABLED = "ServerRouteEnabled";

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
