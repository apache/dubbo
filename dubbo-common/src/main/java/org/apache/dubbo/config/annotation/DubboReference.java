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
package org.apache.dubbo.config.annotation;

import org.apache.dubbo.common.constants.RegistryConstants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used for referencing a Dubbo service
 *
 * @since 2.7.7
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DubboReference {

    /**
     * Interface class, default value is void.class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * Interface class name, default value is empty string
     */
    String interfaceName() default "";

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

    /**
     * Service target URL for direct invocation, if this is specified, then registry center takes no effect.
     */
    String url() default "";

    /**
     * Client transport type, default value is "netty"
     */
    String client() default "";

    /**
     * Whether to enable generic invocation, default value is false
     */
    boolean generic() default false;

    /**
     * When enable, prefer to call local service in the same JVM if it's present, default value is true
     */
    boolean injvm() default true;

    /**
     * Check if service provider is available during boot up, default value is true
     */
    boolean check() default true;

    /**
     * Whether eager initialize the reference bean when all properties are set, default value is false
     */
    boolean init() default false;

    /**
     * Whether to make connection when the client is created, the default value is false
     */
    boolean lazy() default false;

    /**
     * Export an stub service for event dispatch, default value is false.
     * <p>
     * see org.apache.dubbo.rpc.Constants#STUB_EVENT_METHODS_KEY
     */
    boolean stubevent() default false;

    /**
     * Whether to reconnect if connection is lost, if not specify, reconnect is enabled by default, and the interval
     * for retry connecting is 2000 ms
     * <p>
     * see org.apache.dubbo.remoting.Constants#DEFAULT_RECONNECT_PERIOD
     */
    String reconnect() default "";

    /**
     * Whether to stick to the same node in the cluster, the default value is false
     * <p>
     * see Constants#DEFAULT_CLUSTER_STICKY
     */
    boolean sticky() default false;

    /**
     * How the proxy is generated, legal values include: jdk, javassist
     */
    String proxy() default "";

    /**
     * Service stub name, use interface name + Local if not set
     */
    String stub() default "";

    /**
     * Cluster strategy, legal values include: failover, failfast, failsafe, failback, forking
     */
    String cluster() default "";

    /**
     * Maximum connections service provider can accept, default value is 0 - connection is shared
     */
    int connections() default 0;

    /**
     * The callback instance limit peer connection
     * <p>
     * see org.apache.dubbo.rpc.Constants#DEFAULT_CALLBACK_INSTANCES
     */
    int callbacks() default 0;

    /**
     * Callback method name when connected, default value is empty string
     */
    String onconnect() default "";

    /**
     * Callback method name when disconnected, default value is empty string
     */
    String ondisconnect() default "";

    /**
     * Service owner, default value is empty string
     */
    String owner() default "";

    /**
     * Service layer, default value is empty string
     */
    String layer() default "";

    /**
     * Service invocation retry times
     * <p>
     * see Constants#DEFAULT_RETRIES
     */
    int retries() default 2;

    /**
     * Load balance strategy, legal values include: random, roundrobin, leastactive
     * <p>
     * see Constants#DEFAULT_LOADBALANCE
     */
    String loadbalance() default "";

    /**
     * Whether to enable async invocation, default value is false
     */
    boolean async() default false;

    /**
     * Maximum active requests allowed, default value is 0
     */
    int actives() default 0;

    /**
     * Whether the async request has already been sent, the default value is false
     */
    boolean sent() default false;

    /**
     * Service mock name, use interface name + Mock if not set
     */
    String mock() default "";

    /**
     * Whether to use JSR303 validation, legal values are: true, false
     */
    String validation() default "";

    /**
     * Timeout value for service invocation, default value is 0
     */
    int timeout() default 0;

    /**
     * Specify cache implementation for service invocation, legal values include: lru, threadlocal, jcache
     */
    String cache() default "";

    /**
     * Filters for service invocation
     * <p>
     * see Filter
     */
    String[] filter() default {};

    /**
     * Listeners for service exporting and unexporting
     * <p>
     * see ExporterListener
     */
    String[] listener() default {};

    /**
     * Customized parameter key-value pair, for example: {key1, value1, key2, value2}
     */
    String[] parameters() default {};

    /**
     * Application associated name
     */
    String application() default "";

    /**
     * Module associated name
     */
    String module() default "";

    /**
     * Consumer associated name
     */
    String consumer() default "";

    /**
     * Monitor associated name
     */
    String monitor() default "";

    /**
     * Registry associated name
     */
    String[] registry() default {};

    /**
     * The communication protocol of Dubbo Service
     *
     * @return the default value is ""
     * @since 2.6.6
     */
    String protocol() default "";

    /**
     * Service tag name
     */
    String tag() default "";

    /**
     * Service merger
     */
    String merger() default "";

    /**
     * methods support
     */
    Method[] methods() default {};

    /**
     * The id
     *
     * @return default value is empty
     * @since 2.7.3
     */
    String id() default "";

    /**
     * @return The service names that the Dubbo interface subscribed
     * @see RegistryConstants#SUBSCRIBED_SERVICE_NAMES_KEY
     * @since 2.7.8
     */
    String[] services() default {};
}
