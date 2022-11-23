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
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.config.AbstractReferenceConfig;
import org.apache.dubbo.config.ReferenceConfigBase;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used for referencing a Dubbo service.
 * <p>
 * <b>It is recommended to use @DubboReference on the @Bean method in the Java-config class, but not on the fields or setter methods to be injected.</b>
 * </p>
 * <p>
 * Step 1: Register ReferenceBean in Java-config class:
 * <pre class="code">
 * &#64;Configuration
 * public class ReferenceConfiguration {
 *     &#64;Bean
 *     &#64;DubboReference(group = "demo")
 *     public ReferenceBean&lt;HelloService&gt; helloService() {
 *         return new ReferenceBean();
 *     }
 *
 *     &#64;Bean
 *     &#64;DubboReference(group = "demo", interfaceClass = HelloService.class)
 *     public ReferenceBean&lt;GenericService&gt; genericHelloService() {
 *         return new ReferenceBean();
 *     }
 * }
 * </pre>
 * <p>
 * Step 2: Inject ReferenceBean by @Autowired
 * <pre class="code">
 * public class FooController {
 *     &#64;Autowired
 *     private HelloService helloService;
 *
 *     &#64;Autowired
 *     private GenericService genericHelloService;
 * }
 * </pre>
 *
 * @see org.apache.dubbo.config.spring.reference.ReferenceBeanBuilder
 * @see org.apache.dubbo.config.spring.ReferenceBean
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
     *
     * @deprecated Do not need specify generic value, judge by injection type and interface class
     */
    @Deprecated
    boolean generic() default false;

    /**
     * When enable, prefer to call local service in the same JVM if it's present, default value is true
     *
     * @deprecated using scope="local" or scope="remote" instead
     */
    @Deprecated
    boolean injvm() default true;

    /**
     * Check if service provider is available during boot up, default value is true
     */
    boolean check() default true;

    /**
     * Whether eager initialize the reference bean when all properties are set, default value is true ( null as true)
     *
     * @see ReferenceConfigBase#shouldInit()
     */
    boolean init() default true;

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
     * you can use {@link org.apache.dubbo.common.constants.ClusterRules#FAIL_FAST} ……
     */
    String cluster() default ClusterRules.EMPTY;

    /**
     * Maximum connections service provider can accept, default value is 0 - connection is shared
     */
    int connections() default -1;

    /**
     * The callback instance limit peer connection
     * <p>
     * see org.apache.dubbo.rpc.Constants#DEFAULT_CALLBACK_INSTANCES
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
     * <p>
     * see Constants#DEFAULT_RETRIES
     */
    int retries() default -1;

    /**
     * Load balance strategy, legal values include: random, roundrobin, leastactive
     * you can use {@link org.apache.dubbo.common.constants.LoadbalanceRules#RANDOM} ……
     */
    String loadbalance() default LoadbalanceRules.EMPTY;

    /**
     * Whether to enable async invocation, default value is false
     */
    boolean async() default false;

    /**
     * Maximum active requests allowed, default value is 0
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
     * Timeout value for service invocation, default value is 0
     */
    int timeout() default -1;

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
     * Customized parameter key-value pair, for example: {key1, value1, key2, value2} or {"key1=value1", "key2=value2"}
     */
    String[] parameters() default {};

    /**
     * Application name
     *
     * @deprecated This attribute was deprecated, use bind application/module of spring ApplicationContext
     */
    @Deprecated
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
     * NOTE: The id attribute is ignored when using @DubboReference on @Bean method
     *
     * @return default value is empty
     * @since 2.7.3
     */
    String id() default "";

    /**
     * @return The service names that the Dubbo interface subscribed
     * @see RegistryConstants#SUBSCRIBED_SERVICE_NAMES_KEY
     * @since 2.7.8
     * @deprecated using {@link DubboReference#providedBy()}
     */
    @Deprecated
    String[] services() default {};

    /**
     * declares which app or service this interface belongs to
     *
     * @see RegistryConstants#PROVIDED_BY
     */
    String[] providedBy() default {};

    /**
     * The service port of the provider
     *
     * @see AbstractReferenceConfig#providerPort
     * @since 3.1.0
     */
    int providerPort() default -1;

    /**
     * assign the namespace that provider belong to
     * @see AbstractReferenceConfig#providerNamespace
     * @since 3.1.1
     */
    String providerNamespace() default "";

    /**
     * the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
     *
     * @see org.apache.dubbo.rpc.Constants#SCOPE_LOCAL
     * @see org.apache.dubbo.rpc.Constants#SCOPE_REMOTE
     */
    String scope() default "";

    /**
     * Weather the reference is refer asynchronously
     */
    boolean referAsync() default false;

    /**
     * unload Cluster related in mesh mode
     *
     * @see ReferenceConfigBase#unloadClusterRelated
     * @since 3.1.0
     */
    boolean unloadClusterRelated() default false;
}
