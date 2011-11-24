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
package com.alibaba.dubbo.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Constants
 * 
 * @author william.liangf
 */
public class Constants {

    public static final List<String> DEFAULT_TELNET_COMMANDS    = Collections.unmodifiableList(Arrays.asList(new String[] {
            "ls", "ps", "cd", "pwd", "invoke", "count", "trace", "status", "help", "clear", "exit","log" }));

    public static final List<String> DEFAULT_CHECK_STATUSES    = Collections.unmodifiableList(Arrays.asList(new String[] {
            "server", "registry", "threadpool", "datasource", "spring", "memory", "load" }));

    public static final String       SENT_KEY                       = "sent";

    public static final boolean      DEFAULT_SENT                   = false;

    public static final String       REGISTRY_PROTOCOL              = "registry";

    public static final String       $INVOKE                        = "$invoke";

    public static final String       $ECHO                          = "$echo";

    public static final int          DEFAULT_IO_THREADS             = Runtime.getRuntime().availableProcessors() + 1;

    public static final String       DEFAULT_PROXY                  = "javassist";

    public static final int          DEFAULT_PAYLOAD = 8 * 1024 * 1024; // 8M

    public static final String       DEFAULT_CLUSTER                = "failover";
    
    public static final String       DEFAULT_DIRECTORY              = "dubbo";

    public static final String       DEFAULT_LOADBALANCE            = "random";

    public static final String       DEFAULT_PROTOCOL               = "dubbo";

    public static final String       DEFAULT_EXCHANGER              = "header";

    public static final String       DEFAULT_TRANSPORTER            = "netty";

    public static final String       DEFAULT_REMOTING_SERVER        = "netty";

    public static final String       DEFAULT_REMOTING_CLIENT        = "netty";

    public static final String       DEFAULT_REMOTING_CODEC         = "dubbo";

    public static final String       DEFAULT_REMOTING_SERIALIZATION = "hessian2";

    public static final String       DEFAULT_HTTP_SERVER            = "servlet";

    public static final String       DEFAULT_HTTP_CLIENT            = "jdk";

    public static final String       DEFAULT_HTTP_SERIALIZATION     = "json";

    public static final String       DEFAULT_CHARSET                = "UTF-8";

    public static final int          DEFAULT_WEIGHT                 = 5;

    public static final int          DEFAULT_FORKS                  = 2;

    public static final String       DEFAULT_THREAD_NAME            = "Dubbo";

    public static final int          DEFAULT_THREADS                = 100;

    public static final int          DEFAULT_QUEUES                 = 0;

    public static final int          DEFAULT_THREAD_ALIVE           = 60 * 1000;

    public static final int          DEFAULT_CONNECTIONS            = 0;

    public static final int          DEFAULT_ACCEPTS                = 0;

    public static final int          DEFAULT_IDLE_TIMEOUT           = 600 * 1000;

    public static final int          DEFAULT_HEARTBEAT              = 0;

    public static final int          DEFAULT_TIMEOUT                = 5000;

    public static final int          DEFAULT_RETRIES                = 2;

    // default buffer size is 8k.
    public static final int          DEFAULT_BUFFER_SIZE            = 8 * 1024;
    
    public static final int          MAX_BUFFER_SIZE                = 16 * 1024;
    
    public static final int          MIN_BUFFER_SIZE                = 1 * 1024;

    public static final String       REMOVE_VALUE_PREFIX            = "-";
    
    public static final String       HIDE_KEY_PREFIX                = ".";
    
    public static final String       DEFAULT_KEY_PREFIX             = "default.";
    
    public static final String       DEFAULT_KEY                    = "default";

    public static final String       LOADBALANCE_KEY                = "loadbalance";
    
    public static final String       ROUTER_KEY                     = "router";

    public static final String       CLUSTER_KEY                    = "cluster";

    public static final String       REGISTRY_KEY                   = "registry";

    public static final String       MONITOR_KEY                    = "monitor";

    public static final String       DEFAULT_REGISTRY               = "dubbo";

    public static final String       BACKUP_KEY                     = "backup";

    public static final String       DIRECTORY_KEY                  = "directory";

    public static final String       DEPRECATED_KEY                 = "deprecated";
    
    public static final String       ANYHOST_KEY                    = "anyhost";

    public static final String       APPLICATION_KEY                = "application";

    public static final String       LOCAL_KEY                      = "local";

    public static final String       STUB_KEY                       = "stub";

    public static final String       MOCK_KEY                       = "mock";

    public static final String       PROTOCOL_KEY                   = "protocol";

    public static final String       PROXY_KEY                      = "proxy";

    public static final String       WEIGHT_KEY                     = "weight";
    
    public static final String       FORKS_KEY                      = "forks";

    public static final String       DEFAULT_THREADPOOL             = "fixed";

    public static final String       DEFAULT_CLIENT_THREADPOOL      = "cached";

    public static final String       THREADPOOL_KEY                 = "threadpool";

    public static final String       THREAD_NAME_KEY                = "threadname";

    public static final String       IO_THREADS_KEY                 = "iothreads";

    public static final String       THREADS_KEY                    = "threads";

    public static final String       QUEUES_KEY                     = "queues";

    public static final String       THREAD_ALIVE_KEY               = "threadalive";

    public static final String       EXECUTES_KEY                   = "executes";

    public static final String       BUFFER_KEY                     = "buffer";
    
    public static final String       PAYLOAD_KEY                    = "payload";

    public static final String       REFERENCE_FILTER_KEY           = "reference.filter";

    public static final String       INVOKER_LISTENER_KEY           = "invoker.listener";

    public static final String       SERVICE_FILTER_KEY             = "service.filter";

    public static final String       EXPORTER_LISTENER_KEY          = "exporter.listener";

    public static final String       ACCESS_LOG_KEY                 = "accesslog";

    public static final String       ACTIVES_KEY                    = "actives";

    public static final String       CONNECTIONS_KEY                = "connections";

    public static final String       ACCEPTS_KEY                    = "accepts";
    
    public static final String       IDLE_TIMEOUT_KEY               = "idle.timeout";

    public static final String       HEARTBEAT_KEY                  = "heartbeat";

    public static final String       HEARTBEAT_TIMEOUT_KEY          = "heartbeat.timeout";

    public static final String       CONNECT_TIMEOUT_KEY            = "connect.timeout";
    
    public static final String       TIMEOUT_KEY                    = "timeout";

    public static final String       RETRIES_KEY                    = "retries";

    public static final String       CODEC_KEY                      = "codec";
    public static final String       DOWNSTREAM_CODEC_KEY           = "codec.downstream";

    public static final String       SERIALIZATION_KEY              = "serialization";
    
    public static final String       EXCHANGER_KEY                  = "exchanger";

    public static final String       TRANSPORTER_KEY                = "transporter";

    public static final String       SERVER_KEY                     = "server";

    public static final String       CLIENT_KEY                     = "client";

    public static final String       ASYNC_KEY                      = "async";

    public static final String       TOKEN_KEY                      = "token";

    public static final String       METHODS_KEY                    = "methods";

    public static final String       CHARSET_KEY                    = "charset";

    public static final String       RECONNECT_KEY                  = "reconnect";

    public static final String       SEND_RECONNECT_KEY             = "send.reconnect";
    
    public static final int          DEFAULT_RECONNECT_PERIOD       =  2000;
    
    public static final String       SHUTDOWN_TIMEOUT_KEY           =  "shutdown.timeout";
    
    public static final int          DEFAULT_SHUTDOWN_TIMEOUT       =  10000;

    public static final String       CHECK_KEY                      = "check";

    public static final String       GROUP_KEY                      = "group";

    public static final String       PATH_KEY                       = "path";
    
    public static final String       INTERFACE_KEY                  = "interface";
    
    public static final String       GENERIC_KEY                    = "generic";
    
    public static final String       FILE_KEY                       = "file";

    public static final String       WAIT_KEY                       = "wait";

    public static final String       VERSION_KEY                    = "version";

    public static final String       REVISION_KEY                   = "revision";

    public static final String       DUBBO_VERSION_KEY              = "dubbo";

    public static final String       HESSIAN_VERSION_KEY            = "hessian.version";
    
    public static final String       CHANNEL_HANDLER_KEY            = "channel.handler";
    
    public static final String       DEFAULT_CHANNEL_HANDLER        = "default";
    
    public static final String       ANY_VALUE                      = "*";

    public static final String       COMMA_SEPARATOR                = ",";

    public static final Pattern      COMMA_SPLIT_PATTERN            = Pattern.compile("\\s*[,]+\\s*");

    public static final String       REGISTRY_SEPARATOR             = "|";

    public static final Pattern      REGISTRY_SPLIT_PATTERN         = Pattern.compile("\\s*[|]+\\s*");

    public static final String       SEMICOLON_SEPARATOR            = ";";

    public static final Pattern      SEMICOLON_SPLIT_PATTERN        = Pattern.compile("\\s*[;]+\\s*");
    
    public static final String       CONNECT_QUENE_CAPACITY         = "connect.quene.capacity";
    
    public static final String       CONNECT_QUENE_WARNING_SIZE     = "connect.quene.warning.size";
    
    public static final int          DEFAULT_CONNECT_QUENE_WARNING_SIZE  = 1000;
    
    public static final String       CHANNEL_ATTRIBUTE_READONLY_KEY     = "channel.readonly";
    
    public static final String       CHANNEL_READONLYEVENT_SENT_KEY        = "channel.readonly.sent";
    
    public static final String       CHANNEL_SEND_READONLYEVENT_KEY        = "channel.readonly.send";

    public static final String       SUBSCRIBE_PROTOCOL                 = "subscribe";

    public static final String       LOOKUP_PROTOCOL                    = "lookup";

    public static final String       FORBID_PROTOCOL                    = "forbid";

    public static final String       ROUTE_PROTOCOL                     = "route";

    private Constants(){
    }

}