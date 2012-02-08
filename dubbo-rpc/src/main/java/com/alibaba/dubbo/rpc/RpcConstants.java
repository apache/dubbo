/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RpcConstants
 * 
 * @author william.liangf
 */
public final class RpcConstants {
    
    public static final List<String> DEFAULT_REFERENCE_FILTERS = Collections.unmodifiableList(Arrays.asList(new String[] {
            "consumercontext", "deprecated", "collect", "genericimpl", "activelimit", "monitor", "future" }));

    public static final List<String> DEFAULT_SERVICE_FILTERS    = Collections.unmodifiableList(Arrays.asList(new String[] {
            "context", "token", "exception", "echo", "generic", "accesslog", "trace", "classloader", "executelimit", "monitor" ,"timeout"}));

    public static final List<String> DEFAULT_INVOKER_LISTENERS = Collections.unmodifiableList(Arrays.asList(new String[] {
            "deprecated" }));

    public static final List<String> DEFAULT_EXPORTER_LISTENERS = Collections.unmodifiableList(Arrays.asList(new String[] { }));

    /**
     * 集群时是否排除非available的invoker
     */
    public static final String CLUSTER_AVAILABLE_CHECK_KEY = "cluster.availablecheck";
    
    /**
     */
    public static final boolean DEFAULT_CLUSTER_AVAILABLE_CHECK = true;
    
    /**
     * 集群时是否启用sticky策略
     */
    public static final String CLUSTER_STICKY_KEY = "sticky";
    
    /**
     * sticky默认值.
     */
    public static final boolean DEFAULT_CLUSTER_STICKY = false;
    
    /**
     * 创建client时，是否先要建立连接。
     */
    public static final String LAZY_CONNECT_KEY = "lazy";
    
    /**
     * lazy连接的初始状态是连接状态还是非连接状态？
     */
    public static final String LAZY_CONNECT_INITIAL_STATE_KEY = "connect.lazy.initial.state";
    
    /**
     * lazy连接的初始状态默认是连接状态.
     */
    public static final boolean DEFAULT_LAZY_CONNECT_INITIAL_STATE = true;
    
    /**
     * 注册中心是否同步存储文件，默认异步
     */
    public static final String REGISTRY_FILESAVE_SYNC_KEY = "save.file";
    
    /**
     *注册中心失败事件重试事件
     */
    public static final String REGISTRY_RETRY_PERIOD_KEY = "retry.period";
    
    /**
     *注册中心自动重连时间
     */
    public static final String REGISTRY_RECONNECT_PERIOD_KEY = "reconnect.period";
    
    /**
     * 注册中心导出URL参数的KEY
     */
    public static final String EXPORT_KEY = "export";
    
    /**
     * 注册中心引用URL参数的KEY
     */
    public static final String REFER_KEY = "refer";
    
    /**
     * callback inst id
     */
    public static final String CALLBACK_SERVICE_KEY = "callback.service.instid";
    
    /**
     * 每个客户端同一个接口 callback服务实例的限制
     */
    public static final String CALLBACK_INSTANCES_LIMIT_KEY = "callbacks";
    
    /**
     * 每个客户端同一个接口 callback服务实例的限制
     */
    public static final int  DEFAULT_CALLBACK_INSTANCES = 1;
    
    public static final String CALLBACK_SERVICE_PROXY_KEY = "callback.service.proxy";
    
    public static final String IS_CALLBACK_SERVICE = "is_callback_service";
    
    /**
     * channel中callback的invokers 
     */
    public static final String CHANNEL_CALLBACK_KEY = "channel.callback.invokers.key"; 
    
    @Deprecated 
    public static final String SHUTDOWN_TIMEOUT_SECONDS_KEY = "dubbo.service.shutdown.wait.seconds";
    
    public static final String SHUTDOWN_TIMEOUT_KEY = "dubbo.service.shutdown.wait";
    
    public static final String IS_SERVER_KEY = "isserver";
    
    /**
     * 默认值毫秒，避免重新计算.
     */
    public static final int DEFAULT_SERVER_SHUTDOWN_TIMEOUT = 10000;
    
    public static final String ON_CONNECT_KEY = "onconnect";
    
    public static final String ON_DISCONNECT_KEY = "ondisconnect";

    public static final String RETURN_KEY = "return";

    public static final String ON_INVOKE_METHOD_KEY = "oninvoke.method";
    
    public static final String ON_RETURN_METHOD_KEY = "onreturn.method";
    
    public static final String ON_THROW_METHOD_KEY = "onthrow.method";
    
    public static final String ON_INVOKE_INSTANCE_KEY = "oninvoke.instance";
    
    public static final String ON_RETURN_INSTANCE_KEY = "onreturn.instance";
    
    public static final String ON_THROW_INSTANCE_KEY = "onthrow.instance";
    
    public static final String OVERWRIDE_PROTOCOL = "overwride";
    
    public static final String ROUTE_PROTOCOL = "route";
    
    public static final String RULE_KEY = "rule";

    public static final String TYPE_KEY = "type";
    
    // key for router type, for e.g., "script"/"file",  corresponding to ScriptRouterFactory.NAME, FileRouterFactory.NAME 
    public static final String ROUTER_KEY = "router";

    // when ROUTER_KEY's value is set to ROUTER_TYPE_CLEAR, RegistryDirectory will clean all current routers
    public static final String ROUTER_TYPE_CLEAR = "clean";

    public static final String DEFAULT_SCRIPT_TYPE_KEY = "javascript";
    
    public static final String STUB_EVENT_KEY = "dubbo.stub.event";
    
    public static final boolean DEFAULT_STUB_EVENT = false;
    
    public static final String STUB_EVENT_METHODS_KEY = "dubbo.stub.event.methods";
    
    private RpcConstants() {}
    
}