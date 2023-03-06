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


import org.apache.dubbo.common.constants.ClusterRules;
import org.apache.dubbo.common.constants.LoadbalanceRules;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level annotation used for declaring Dubbo service.
 * <p/>
 * <b>1. Using with java config bean:</b>
 * <p/>
 * <b>This usage is recommended</b>.<br/>
 * It is more flexible on bean methods than on implementation classes, and is more compatible with Spring.
 * <pre>
 * &#64;Configuration
 * class ProviderConfiguration {
 *
 *     &#64;Bean
 *     &#64;DubboService(group="demo")
 *     public DemoService demoServiceImpl() {
 *         return new DemoServiceImpl();
 *     }
 * }
 * </pre>
 *
 * <b>2. Using on implementation class of service:  </b>
 * <pre>
 * &#64;DubboService(group="demo")
 * public class DemoServiceImpl implements DemoService {
 *     ...
 * }
 * </pre>
 *
 * This usage causes the implementation class to rely on the Dubbo module.
 *
 *
 * @since 2.7.7
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface DubboService {

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
     * Service path, default value is empty string
     */
    String path() default "";

    /**
     * Whether to export service, default value is true
     */
    boolean export() default true;

    /**
     * Service token, default value is empty string
     */
    String token() default "";

    /**
     * Whether the service is deprecated, default value is false
     */
    boolean deprecated() default false;

    /**
     * Whether the service is dynamic, default value is true
     */
    boolean dynamic() default true;

    /**
     * Access log for the service, default value is empty string
     */
    String accesslog() default "";

    /**
     * Maximum concurrent executes for the service, default value is -1 - no limits
     */
    int executes() default -1;

    /**
     * Whether to register the service to register center, default value is true
     */
    boolean register() default true;

    /**
     * Service weight value, default value is -1
     */
    int weight() default -1;

    /**
     * Service doc, default value is empty string
     */
    String document() default "";

    /**
     * Delay time for service registration, default value is -1
     */
    int delay() default -1;

    /**
     * @see DubboService#stub()
     * @deprecated
     */
    String local() default "";

    /**
     * Service stub name, use interface name + Local if not set
     */
    String stub() default "";

    /**
     * Cluster strategy, legal values include: failover, failfast, failsafe, failback, forking
     * you can use {@link org.apache.dubbo.common.constants.ClusterRules#FAIL_FAST} ……
     */
    String cluster() default ClusterRules.EMPTY;

    /**
     * How the proxy is generated, legal values include: jdk, javassist
     */
    String proxy() default "";

    /**
     * Maximum connections service provider can accept, default value is -1 - connection is shared
     */
    int connections() default -1;

    /**
     * The callback instance limit peer connection
     * <p>
     * see org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CALLBACK_INSTANCES
     */
    int callbacks() default -1;

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
     *
     * @see org.apache.dubbo.common.constants.CommonConstants#DEFAULT_RETRIES
     */
    int retries() default -1;

    /**
     * Load balance strategy, legal values include: random, roundrobin, leastactive
     *
     * you can use {@link org.apache.dubbo.common.constants.LoadbalanceRules#RANDOM} ……
     */
    String loadbalance() default LoadbalanceRules.EMPTY;

    /**
     * Whether to enable async invocation, default value is false
     */
    boolean async() default false;

    /**
     * Maximum active requests allowed, default value is -1
     */
    int actives() default -1;

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
     * Timeout value for service invocation, default value is -1
     */
    int timeout() default -1;

    /**
     * Specify cache implementation for service invocation, legal values include: lru, threadlocal, jcache
     */
    String cache() default "";

    /**
     * Filters for service invocation
     *
     * @see Filter
     */
    String[] filter() default {};

    /**
     * Listeners for service exporting and unexporting
     *
     * @see ExporterListener
     */
    String[] listener() default {};

    /**
     * Customized parameter key-value pair, for example: {key1, value1, key2, value2}
     */
    String[] parameters() default {};

    /**
     * Application spring bean name
     * @deprecated This attribute was deprecated, use bind application/module of spring ApplicationContext
     */
    @Deprecated
    String application() default "";

    /**
     * Module spring bean name
     */
    String module() default "";

    /**
     * Provider spring bean name
     */
    String provider() default "";

    /**
     * Protocol spring bean names
     */
    String[] protocol() default {};

    /**
     * Monitor spring bean name
     */
    String monitor() default "";

    /**
     * Registry spring bean name
     */
    String[] registry() default {};

    /**
     * Service tag name
     */
    String tag() default "";

    /**
     * methods support
     *
     * @return
     */
    Method[] methods() default {};

    /**
     * the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
     * @see org.apache.dubbo.rpc.Constants#SCOPE_LOCAL
     * @see org.apache.dubbo.rpc.Constants#SCOPE_REMOTE
     */
    String scope() default "";

    /**
     * Weather the service is export asynchronously
     */
    boolean exportAsync() default false;

    /**
     * bean name of service executor(thread pool), used for thread pool isolation between services
     * @return
     */
    String executor() default "";

    /**
     * Payload max length.
     */
    String payload() default "";
}
