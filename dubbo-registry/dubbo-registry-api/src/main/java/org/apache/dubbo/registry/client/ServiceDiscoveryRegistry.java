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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils;
import org.apache.dubbo.registry.client.metadata.SubscribedURLsSynthesizer;
import org.apache.dubbo.registry.client.metadata.proxy.MetadataServiceProxyFactory;
import org.apache.dubbo.registry.client.selector.ServiceInstanceSelector;
import org.apache.dubbo.registry.support.FailbackRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.dubbo.common.URLBuilder.from;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR_CHAR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PID_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.function.ThrowableAction.execute;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.StringUtils.splitToSet;
import static org.apache.dubbo.metadata.MetadataService.toURLs;
import static org.apache.dubbo.registry.client.ServiceDiscoveryFactory.getExtension;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getExportedServicesRevision;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getMetadataStorageType;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.getProtocolPort;

/**
 * Being different to the traditional registry, {@link ServiceDiscoveryRegistry} that is a new service-oriented
 * {@link Registry} based on {@link ServiceDiscovery}, it will not interact in the external registry directly,
 * but store the {@link URL urls} that Dubbo services exported and referenced into {@link WritableMetadataService}
 * when {@link #register(URL)} and {@link #subscribe(URL, NotifyListener)} methods are executed. After that the exported
 * {@link URL urls} can be get from {@link WritableMetadataService#getExportedURLs()} and its variant methods. In contrast,
 * {@link WritableMetadataService#getSubscribedURLs()} method offers the subscribed {@link URL URLs}.
 * <p>
 * Every {@link ServiceDiscoveryRegistry} object has its own {@link ServiceDiscovery} instance that was initialized
 * under {@link #ServiceDiscoveryRegistry(URL) the construction}. As the primary argument of constructor , the
 * {@link URL} of connection the registry decides what the kind of ServiceDiscovery is. Generally, each
 * protocol associates with a kind of {@link ServiceDiscovery}'s implementation if present, or the
 * {@link FileSystemServiceDiscovery} will be the default one. Obviously, it's also allowed to extend
 * {@link ServiceDiscovery} using {@link SPI the Dubbo SPI}.
 * <p>
 * In the {@link #subscribe(URL, NotifyListener) subscription phase}, the {@link ServiceDiscovery} instance will be used
 * to discovery the {@link ServiceInstance service instances} via the {@link ServiceDiscovery#getInstances(String)}.
 * However, the argument of this method requires the service name that the subscribed {@link URL} can't find, thus,
 * {@link ServiceNameMapping} will help to figure out one or more services that exported correlative Dubbo services. If
 * the service names can be found, the exported {@link URL URLs} will be get from the remote {@link MetadataService}
 * being deployed on all {@link ServiceInstance instances} of services. The whole process runs under the
 * {@link #subscribeURLs(URL, List, String, Collection)} method. It's very expensive to invoke
 * {@link MetadataService} for each {@link ServiceInstance service instance}, thus {@link ServiceDiscoveryRegistry}
 * introduces a cache to optimize the calculation with "revisions". If the revisions of N
 * {@link ServiceInstance service instances} are same, {@link MetadataService} is invoked just only once, and then it
 * does return the exported {@link URL URLs} as a template by which others are
 * {@link #cloneExportedURLs(URL, Collection) cloned}.
 * <p>
 * In contrast, current {@link ServiceInstance service instance} will not be registered to the registry whether any
 * Dubbo service is exported or not.
 * <p>
 *
 * @see ServiceDiscovery
 * @see FailbackRegistry
 * @see WritableMetadataService
 * @since 2.7.5
 */
public class ServiceDiscoveryRegistry extends FailbackRegistry {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceDiscovery serviceDiscovery;

    private Set<String> subscribedServices;

    private final ServiceNameMapping serviceNameMapping;

    private final WritableMetadataService writableMetadataService;

    private final Set<String> registeredListeners = new LinkedHashSet<>();

    private final List<SubscribedURLsSynthesizer> subscribedURLsSynthesizers;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * A cache for all URLs of services that the subscribed services exported
     * The key is the service name
     * The value is a nested {@link Map} whose key is the revision and value is all URLs of services
     *
     * Map<serviceName, Map<revision, List<暴露服务对应的URL>>>
     */
    private final Map<String, Map<String, List<URL>>> serviceRevisionExportedURLsCache = new LinkedHashMap<>();

    public ServiceDiscoveryRegistry(URL registryURL) {
        super(registryURL);
        // 实例化registryURL对应的serviceDiscovery  并初始化
        this.serviceDiscovery = createServiceDiscovery(registryURL);
        // 获取默认实现 CompositeServiceNameMapping
        this.serviceNameMapping = ServiceNameMapping.getDefaultExtension();
        String metadataStorageType = getMetadataStorageType(registryURL);
        // 获取metadataStorageType对应的WritableMetadataService实现
        this.writableMetadataService = WritableMetadataService.getExtension(metadataStorageType);
        // spi  获取SubscribedURLsSynthesizer的实现
        this.subscribedURLsSynthesizers = initSubscribedURLsSynthesizers();
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    /**
     * Get the subscribed services from the specified registry {@link URL url}
     *
     * @param registryURL the specified registry {@link URL url}
     * @return non-null
     */
    public static Set<String> getSubscribedServices(URL registryURL) {
        return parseServices(registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY));
    }

    /**
     * Create the {@link ServiceDiscovery} from the registry {@link URL}
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return non-null
     */
    protected ServiceDiscovery createServiceDiscovery(URL registryURL) {
        /**
         * 获取registryURL中协议对应的ServiceDiscovery实现
         */
        ServiceDiscovery originalServiceDiscovery = getServiceDiscovery(registryURL);
        // NacosServiceDiscovery
        ServiceDiscovery serviceDiscovery = enhanceEventPublishing(originalServiceDiscovery);
        execute(() -> {
            // 初始化   EventPublishingServiceDiscovery -> NacosServiceDiscovery
            serviceDiscovery.initialize(registryURL.addParameter(INTERFACE_KEY, ServiceDiscovery.class.getName())
                    .removeParameter(REGISTRY_TYPE_KEY));
        });
        return serviceDiscovery;
    }

    private List<SubscribedURLsSynthesizer> initSubscribedURLsSynthesizers() {
        ExtensionLoader<SubscribedURLsSynthesizer> loader = ExtensionLoader.getExtensionLoader(SubscribedURLsSynthesizer.class);
        return Collections.unmodifiableList(new ArrayList<>(loader.getSupportedExtensionInstances()));
    }

    /**
     * Get the instance {@link ServiceDiscovery} from the registry {@link URL} using
     * {@link ServiceDiscoveryFactory} SPI
     *
     * @param registryURL the {@link URL} to connect the registry
     * @return
     */
    private ServiceDiscovery getServiceDiscovery(URL registryURL) {
        ServiceDiscoveryFactory factory = getExtension(registryURL);
        return factory.getServiceDiscovery(registryURL);
    }

    /**
     * Enhance the original {@link ServiceDiscovery} with event publishing feature
     *
     * @param original the original {@link ServiceDiscovery}
     * @return {@link EventPublishingServiceDiscovery} instance
     */
    private ServiceDiscovery enhanceEventPublishing(ServiceDiscovery original) {
        return new EventPublishingServiceDiscovery(original);
    }

    /**
     * dubbo://2.0.1.17:20885/org.apache.dubbo.config.bootstrap.EchoService?anyhost=true&application=dubbo-nacos-provider-demo&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=test&interface=org.apache.dubbo.config.bootstrap.EchoService&metadata-type=remote&methods=echo&pid=6320&release=&revision=1.0.1&side=provider&timestamp=1604907434619&version=1.0.1
     * @param providerURL
     * @return
     */
    protected boolean shouldRegister(URL providerURL) {
        // 获取side对应的值
        String side = providerURL.getParameter(SIDE_KEY);
        //side对应的值是否为provider
        boolean should = PROVIDER_SIDE.equals(side); // Only register the Provider.

        if (!should) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("The URL[%s] should not be registered.", providerURL.toString()));
            }
        }

        return should;
    }

    protected boolean shouldSubscribe(URL subscribedURL) {
        return !shouldRegister(subscribedURL);
    }

    @Override
    public final void register(URL url) {
        /**
         * url对应的side值非provider 则返回
         */
        if (!shouldRegister(url)) { // Should Not Register
            return;
        }
        super.register(url);
    }

    @Override
    public void doRegister(URL url) {
        /**
         * 本地缓存url  InMemoryWritableMetadataService
         */
        if (writableMetadataService.exportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(format("The URL[%s] registered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.info(format("The URL[%s] has been registered.", url.toString()));
            }
        }
    }

    @Override
    public final void unregister(URL url) {
        if (!shouldRegister(url)) {
            return;
        }
        super.unregister(url);
    }

    @Override
    public void doUnregister(URL url) {
        if (writableMetadataService.unexportURL(url)) {
            if (logger.isInfoEnabled()) {
                logger.info(format("The URL[%s] deregistered successfully.", url.toString()));
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.info(format("The URL[%s] has been deregistered.", url.toString()));
            }
        }
    }

    /**
     * 服务自省  订阅
     * @param url
     * @param listener
     */
    @Override
    public final void subscribe(URL url, NotifyListener listener) {
        // 过滤
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        /**
         * 订阅
         */
        super.subscribe(url, listener);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        subscribeURLs(url, listener);
    }

    @Override
    public final void unsubscribe(URL url, NotifyListener listener) {
        if (!shouldSubscribe(url)) { // Should Not Subscribe
            return;
        }
        super.unsubscribe(url, listener);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        writableMetadataService.unsubscribeURL(url);
    }

    @Override
    public boolean isAvailable() {
        return !serviceDiscovery.getServices().isEmpty();
    }

    @Override
    public void destroy() {
        super.destroy();
        execute(() -> {
            // stop ServiceDiscovery
            serviceDiscovery.destroy();
        });
    }

    protected void subscribeURLs(URL url, NotifyListener listener) {
        // 本地元数据中心缓存订阅的url
        writableMetadataService.subscribeURL(url);

        /**
         * 获取服务消费者url对应的服务提供者所在的serviceNames
         */
        Set<String> serviceNames = getServices(url);

        List<URL> subscribedURLs = new LinkedList<>();

        serviceNames.forEach(serviceName -> {
            /**
             * 获取服务消费端对应得服务提供端服务得url列表  填充到subscribedURLs
             * 需要访问服务提供端得元数据服务
             */
            subscribeURLs(url, subscribedURLs, serviceName);

            // register ServiceInstancesChangedListener
            registerServiceInstancesChangedListener(url, new ServiceInstancesChangedListener(serviceName) {

                @Override
                public void onEvent(ServiceInstancesChangedEvent event) {
                    List<URL> subscribedURLs = new LinkedList<>();
                    Set<String> others = new HashSet<>(serviceNames);
                    others.remove(serviceName);

                    // Collect the subscribedURLs
                    subscribeURLs(url, subscribedURLs, serviceName, () -> event.getServiceInstances());
                    subscribeURLs(url, subscribedURLs, others.toString(), () -> getServiceInstances(others));

                    // Notify all
                    notifyAllSubscribedURLs(url, subscribedURLs, listener);

                }
            });
        });

        // Notify all
        /**
         * 同非服务自省  启动消费端invoker  并更新内部invoker缓存
         */
        notifyAllSubscribedURLs(url, subscribedURLs, listener);

    }

    /**
     *
     * @param url 本地订阅的url  consumer://192.168.50.39/org.apache.dubbo.rpc.service.GenericService?application=dubbo-demo-api-consumer&category=providers,configurators,routers&check=false&dubbo=2.0.2&generic=true&interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&pid=13208&side=consumer&sticky=false&timestamp=1605687787066
     * @param subscribedURLs  服务提供端暴露服务的url  dubbo://2.0.1.2:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&default=true&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&methods=sayHello,sayHelloAsync&release=&side=provider
     * @param listener 监听
     */
    private void notifyAllSubscribedURLs(URL url, List<URL> subscribedURLs, NotifyListener listener) {

        if (subscribedURLs.isEmpty()) {
            // Add the EMPTY_PROTOCOL URL
            subscribedURLs.add(from(url).setProtocol(EMPTY_PROTOCOL).removeParameter(CATEGORY_KEY).build());
        }

        // Notify all
        /**
         * RegistryDirectory
         * 同非服务自省  启动消费端invoker  并更新内部invoker缓存
         */
        listener.notify(subscribedURLs);
    }

    private List<ServiceInstance> getServiceInstances(Set<String> serviceNames) {
        if (isEmpty(serviceNames)) {
            return emptyList();
        }
        List<ServiceInstance> allServiceInstances = new LinkedList<>();
        for (String serviceName : serviceNames) {
            List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);
            if (!isEmpty(serviceInstances)) {
                allServiceInstances.addAll(serviceInstances);
            }
        }
        return allServiceInstances;
    }

    protected void subscribeURLs(URL subscribedURL, List<URL> subscribedURLs,
                                 String serviceName, Supplier<Collection<ServiceInstance>> serviceInstancesSupplier) {
        Collection<ServiceInstance> serviceInstances = serviceInstancesSupplier.get();
        subscribeURLs(subscribedURL, subscribedURLs, serviceName, serviceInstances);
    }


    protected void subscribeURLs(URL url, List<URL> subscribedURLs, String serviceName) {
        /**
         * EventPublishingServiceDiscovery
         * 在注册中心获取serviceName对应的服务实例
         */
        List<ServiceInstance> serviceInstances = serviceDiscovery.getInstances(serviceName);
        /**
         * 获取服务消费端对应得服务提供端服务得url列表  填充到subscribedURLs
         */
        subscribeURLs(url, subscribedURLs, serviceName, serviceInstances);
    }

    /**
     * Register the {@link ServiceInstancesChangedListener} If absent
     *
     * @param url      {@link URL}
     * @param listener the {@link ServiceInstancesChangedListener}
     */
    private void registerServiceInstancesChangedListener(URL url, ServiceInstancesChangedListener listener) {
        // dubbo-demo-api-provider:consumer://192.168.50.39/org.apache.dubbo.rpc.service.GenericService
        String listenerId = createListenerId(url, listener);
        if (registeredListeners.add(listenerId)) {
            /**
             * EventPublishingServiceDiscovery
             * 添加注册中心监听
             */
            serviceDiscovery.addServiceInstancesChangedListener(listener);
        }
    }

    private String createListenerId(URL url, ServiceInstancesChangedListener listener) {
        return listener.getServiceName() + ":" + url.toString(VERSION_KEY, GROUP_KEY, PROTOCOL_KEY);
    }

    /**
     * Subscribe the {@link URL URLs} that the specified service exported are
     * {@link #getExportedURLs(ServiceInstance) get} from {@link MetadataService} if present, or try to
     * be {@link #synthesizeSubscribedURLs(URL, Collection) synthesized} by
     * the instances of {@link SubscribedURLsSynthesizer}
     *
     * @param subscribedURL    the subscribed {@link URL url}
     * @param subscribedURLs   {@link NotifyListener}
     * @param serviceName
     * @param serviceInstances
     * @see #getExportedURLs(URL, Collection)
     * @see #synthesizeSubscribedURLs(URL, Collection)
     */
    protected void subscribeURLs(URL subscribedURL, List<URL> subscribedURLs, String serviceName,
                                 Collection<ServiceInstance> serviceInstances) {
        //非空
        if (isEmpty(serviceInstances)) {
            logger.warn(format("There is no instance in service[name : %s]", serviceName));
            return;
        }

        /**
         * Add the exported URLs from {@link MetadataService}
         *
         * getExportedURLs
         * 获取服务消费端对应得服务提供端服务得url列表
         * 从注册中心中获取服务消费端得元数据服务   获取对应得服务提供端暴露服务得url列表
         */
        subscribedURLs.addAll(getExportedURLs(subscribedURL, serviceInstances));

        if (subscribedURLs.isEmpty()) { // If empty, try to synthesize
            /**
             * Add the subscribed URLs that were synthesized
             */
            subscribedURLs.addAll(synthesizeSubscribedURLs(subscribedURL, serviceInstances));
        }
    }

    /**
     * Get the exported {@link URL URLs} from the  {@link MetadataService} in the specified
     * {@link ServiceInstance service instances}
     *
     * @param subscribedURL the subscribed {@link URL url}
     * @param instances     {@link ServiceInstance service instances}
     * @return the exported {@link URL URLs} if present, or <code>{@link Collections#emptyList() empty list}</code>
     */
    private List<URL> getExportedURLs(URL subscribedURL, Collection<ServiceInstance> instances) {

        // local service instances could be mutable
        /**
         * 过滤   可用的&&健康的&&dubbo元数据实例
         */
        List<ServiceInstance> serviceInstances = instances.stream()
                .filter(ServiceInstance::isEnabled)
                .filter(ServiceInstance::isHealthy)
                .filter(ServiceInstanceMetadataUtils::isDubboServiceInstance)
                .collect(Collectors.toList());

        int size = serviceInstances.size();

        if (size == 0) {
            return emptyList();
        }

        // Prepare revision exported URLs
        /**
         * 访问serviceInstances对应的元数据服务   获取服务提供端暴露的服务   并缓存
         * 缓存格式为：Map<serviceName, Map<revision, List<暴露服务对应的URL>>>
         */
        prepareServiceRevisionExportedURLs(serviceInstances);

        // Clone the subscribed URLs from the template URLs
        /**
         * 获取服务消费端对应得服务提供端服务得url列表
         */
        List<URL> subscribedURLs = cloneExportedURLs(subscribedURL, serviceInstances);

        // clear local service instances
        serviceInstances.clear();

        return subscribedURLs;
    }

    /**
     * Prepare the {@link #serviceRevisionExportedURLsCache} exclusively
     *
     * @param serviceInstances {@link ServiceInstance service instances}
     * @see #expungeStaleRevisionExportedURLs(List)
     * @see #initializeRevisionExportedURLs(List)
     */
    private void prepareServiceRevisionExportedURLs(List<ServiceInstance> serviceInstances) {
        executeExclusively(() -> {
            // 1. expunge stale
            /**
             * 根据注册中心上最新得ServiceInstance    更新本地缓存中的Revisions对应得服务端暴露url列表
             * 根据注册中心上最新得ServiceInstance    更新本地缓存中的Revisions对应得服务端暴露url列表
             * 根据注册中心上最新得ServiceInstance    更新本地缓存中的Revisions对应得服务端暴露url列表
             */
            expungeStaleRevisionExportedURLs(serviceInstances);
            // 2. Initialize
            /**
             * 访问serviceInstances对应的元数据服务   获取服务提供端暴露的服务   并缓存
             * 缓存格式为：Map<serviceName, Map<revision, List<暴露服务对应的URL>>>
             */
            initializeRevisionExportedURLs(serviceInstances);
        });
    }

    /**
     * Initialize the {@link URL URLs} that {@link ServiceInstance service instances} exported into
     * {@link #serviceRevisionExportedURLsCache the cache}.
     * <p>
     * Typically, the {@link URL URLs} that one {@link ServiceInstance service instance} exported can be get from
     * the same instances' {@link MetadataService}, but the cost is very expensive if there are a lot of instances
     * in this service. Thus, the exported {@link URL URls} should be cached  and stored into
     * {@link #serviceRevisionExportedURLsCache the cache}.
     * <p>
     * In most cases, {@link #serviceRevisionExportedURLsCache the cache} only holds a single list of exported URLs for
     * each service because there is no difference on the Dubbo services(interfaces) between the service instances.
     * However, if there are one or more upgrading or increasing Dubbo services that are deploying on the some of
     * instances, other instances still maintain the previous ones, in this way, there are two versions of the services,
     * they are called "revisions", in other words, one revision associates a list of exported URLs that can be reused
     * for other instances with same revision, and one service allows one or more revisions.
     *
     * @param serviceInstances {@link ServiceInstance service instances}
     */
    private void initializeRevisionExportedURLs(List<ServiceInstance> serviceInstances) {
        // initialize the revision exported URLs that the selected service instance exported
        /**
         * 访问列表中任意一个服务端元数据服务  获取服务端暴露得接口url并缓存 【servicename:{rversion:url}】
         */
        initializeSelectedRevisionExportedURLs(serviceInstances);
        // initialize the revision exported URLs that other service instances exported
        /**
         * 访问全部得serviceInstances对应得元数据中心   获取服务端暴露得接口url并缓存 【servicename:{rversion:url}】
         */
        serviceInstances.forEach(this::initializeRevisionExportedURLs);
    }

    /**
     * Initialize the {@link URL URLs} that the {@link #selectServiceInstance(List) selected service instance} exported
     * into {@link #serviceRevisionExportedURLsCache the cache}.
     *
     * @param serviceInstances {@link ServiceInstance service instances}
     */
    private void initializeSelectedRevisionExportedURLs(List<ServiceInstance> serviceInstances) {
        // Try to initialize revision exported URLs until success
        for (int i = 0; i < serviceInstances.size(); i++) {
            // select a instance of {@link ServiceInstance}
            /**
             * 在serviceInstances列表中随机选取一个ServiceInstance
             */
            ServiceInstance selectedInstance = selectServiceInstance(serviceInstances);
            /**
             * 访问服务提供端元数据服务  获取提供端暴露得接口url
             */
            List<URL> revisionExportedURLs = initializeRevisionExportedURLs(selectedInstance);
            if (isNotEmpty(revisionExportedURLs)) {    // If the result is valid
                break;
            }
        }
    }

    /**
     * Expunge the revision exported {@link URL URLs} in {@link #serviceRevisionExportedURLsCache the cache} if
     * some revisions of {@link ServiceInstance service instance} had been out of date possibly
     *
     * @param serviceInstances {@link ServiceInstance service instances}
     */
    private void expungeStaleRevisionExportedURLs(List<ServiceInstance> serviceInstances) {
        // 获取第一个serviceName
        String serviceName = serviceInstances.get(0).getServiceName();
        // revisionExportedURLsMap is mutable
        // 在serviceRevisionExportedURLsCache中缓存serviceName  并获取之前的值
        Map<String, List<URL>> revisionExportedURLsMap = getRevisionExportedURLsMap(serviceName);

        if (revisionExportedURLsMap.isEmpty()) { // if empty, return immediately
            return;
        }

        //缓存中的rversion集合
        Set<String> existedRevisions = revisionExportedURLsMap.keySet(); // read-only
        /**
         * 获取serviceInstances中服务实例对应的rversion
         */
        Set<String> currentRevisions = serviceInstances.stream()
                // 获取rversion
                .map(ServiceInstanceMetadataUtils::getExportedServicesRevision)
                .collect(Collectors.toSet());
        // staleRevisions = existedRevisions(copy) - currentRevisions
        Set<String> staleRevisions = new HashSet<>(existedRevisions);
        // 移除现有（注册中心获取）的Revisions  即剩下的都是过期的或已失效的Revisions
        staleRevisions.removeAll(currentRevisions);
        // remove exported URLs if staled
        // 在缓存中移除对应的Revisions
        staleRevisions.forEach(revisionExportedURLsMap::remove);
    }

    /**
     * Clone the exported URLs that are based on {@link #getTemplateExportedURLs(URL, ServiceInstance) the template URLs}
     * from the some of {@link ServiceInstance service instances} with different revisions
     *
     * @param subscribedURL    the subscribed {@link URL url}
     * @param serviceInstances {@link ServiceInstance service instances}
     * @return non-null
     */
    private List<URL> cloneExportedURLs(URL subscribedURL, Collection<ServiceInstance> serviceInstances) {

        if (isEmpty(serviceInstances)) {
            return emptyList();
        }

        List<URL> clonedExportedURLs = new LinkedList<>();

        serviceInstances.forEach(serviceInstance -> {

            String host = serviceInstance.getHost();

            /**
             * getTemplateExportedURLs   当前服务消费者可以消费得服务url列表
             */
            getTemplateExportedURLs(subscribedURL, serviceInstance)
                    .stream()
                    .map(templateURL -> templateURL.removeParameter(TIMESTAMP_KEY))
                    .map(templateURL -> templateURL.removeParameter(PID_KEY))
                    .map(templateURL -> {
                        // 获取url对应得protocol
                        String protocol = templateURL.getProtocol();
                        // 获取protocol对应得port
                        int port = getProtocolPort(serviceInstance, protocol);
                        if (Objects.equals(templateURL.getHost(), host)
                                && Objects.equals(templateURL.getPort(), port)) { // use templateURL if equals
                            return templateURL;
                        }

                        // 重组url
                        URLBuilder clonedURLBuilder = from(templateURL) // remove the parameters from the template URL
                                .setHost(host)  // reset the host
                                .setPort(port); // reset the port

                        return clonedURLBuilder.build();
                    })
                    .forEach(clonedExportedURLs::add);
        });
        return clonedExportedURLs;
    }


    /**
     * Select one {@link ServiceInstance} by {@link ServiceInstanceSelector the strategy} if there are more that one
     * instances in order to avoid the hot spot appearing the some instance
     *
     * @param serviceInstances the {@link List list} of {@link ServiceInstance}
     * @return <code>null</code> if <code>serviceInstances</code> is empty.
     * @see ServiceInstanceSelector
     */
    private ServiceInstance selectServiceInstance(List<ServiceInstance> serviceInstances) {
        int size = serviceInstances.size();
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return serviceInstances.get(0);
        }
        /**
         * 如果有多个服务实例，请通过linkserviceInstanceSelector策略选择一个服务实例，以避免热点出现某个实例
         * 默认随机
         */
        ServiceInstanceSelector selector = getExtensionLoader(ServiceInstanceSelector.class).getAdaptiveExtension();
        return selector.select(getUrl(), serviceInstances);
    }

    /**
     * Get the template exported {@link URL urls} from the specified {@link ServiceInstance}.
     * <p>
     * First, put the revision {@link ServiceInstance service instance}
     * associating {@link #getExportedURLs(ServiceInstance) exported URLs} into cache.
     * <p>
     * And then compare a new {@link ServiceInstance service instances'} revision with cached one,If they are equal,
     * return the cached template {@link URL urls} immediately, or to get template {@link URL urls} that the provider
     * {@link ServiceInstance instance} exported via executing {@link ##getExportedURLs(ServiceInstance) (ServiceInstance)}
     * method.
     * <p>
     * Eventually, the retrieving result will be cached and returned.
     *
     * @param subscribedURL    the subscribed {@link URL url}
     * @param selectedInstance the {@link ServiceInstance}
     *                         associating with the {@link URL urls}
     * @return non-null {@link List} of {@link URL urls}
     */
    private List<URL> getTemplateExportedURLs(URL subscribedURL, ServiceInstance selectedInstance) {

        /**
         * 获取selectedInstance对应得服务提供端暴露得服务
         */
        List<URL> exportedURLs = getRevisionExportedURLs(selectedInstance);

        if (isEmpty(exportedURLs)) {
            return emptyList();
        }

        /**
         * 当前服务消费者可以消费得服务列表
         */
        return filterSubscribedURLs(subscribedURL, exportedURLs);
    }

    /**
     * Initialize the URLs that the specified {@link ServiceInstance service instance} exported
     *
     * @param serviceInstance the {@link ServiceInstance} exports the Dubbo Services
     * @return the {@link URL URLs} that the {@link ServiceInstance} exported, it's calculated from
     * The invocation to remote {@link MetadataService}, or get from {@link #serviceRevisionExportedURLsCache cache} if
     * {@link ServiceInstanceMetadataUtils#getExportedServicesRevision(ServiceInstance) revision} is hit
     */
    private List<URL> initializeRevisionExportedURLs(ServiceInstance serviceInstance) {

        if (serviceInstance == null) {
            return emptyList();
        }

        String serviceName = serviceInstance.getServiceName();
        // get the revision from the specified {@link ServiceInstance}
        String revision = getExportedServicesRevision(serviceInstance);

        /**
         * 缓存中获取serviceName对应得revision和url
         */
        Map<String, List<URL>> revisionExportedURLsMap = getRevisionExportedURLsMap(serviceName);

        /**
         * 获取revision对应得url
         */
        List<URL> revisionExportedURLs = revisionExportedURLsMap.get(revision);

        boolean firstGet = false;
        // 缓存未命中
        if (revisionExportedURLs == null) { // The hit is missing in cache

            if (!revisionExportedURLsMap.isEmpty()) { // The case is that current ServiceInstance with the different revision
                if (logger.isWarnEnabled()) {
                    logger.warn(format("The ServiceInstance[id: %s, host : %s , port : %s] has different revision : %s" +
                                    ", please make sure the service [name : %s] is changing or not.",
                            serviceInstance.getId(),
                            serviceInstance.getHost(),
                            serviceInstance.getPort(),
                            revision,
                            serviceInstance.getServiceName()
                    ));
                }
            } else { // Else, it's the first time to get the exported URLs
                firstGet = true;
            }

            /**
             * 访问服务提供端元数据服务  获取提供端暴露得接口url
             * 访问服务提供端元数据服务  获取提供端暴露得接口url
             * 访问服务提供端元数据服务  获取提供端暴露得接口url
             */
            revisionExportedURLs = getExportedURLs(serviceInstance);

            if (revisionExportedURLs != null) { // just allow the valid result into exportedURLsMap
                /**
                 * 在缓存中  修改revision对应服务端暴露url列表
                 */
                revisionExportedURLsMap.put(revision, revisionExportedURLs);

                if (logger.isDebugEnabled()) {
                    logger.debug(format("Get the exported URLs[size : %s, first : %s] from the target service " +
                                    "instance [id: %s , service : %s , host : %s , port : %s , revision : %s]",
                            revisionExportedURLs.size(), firstGet,
                            serviceInstance.getId(),
                            serviceInstance.getServiceName(),
                            serviceInstance.getHost(),
                            serviceInstance.getPort(),
                            revision
                    ));
                }
            }
        } else { // Else, The cache is hit
            if (logger.isDebugEnabled()) {
                logger.debug(format("Get the exported URLs[size : %s] from cache, the instance" +
                                "[id: %s , service : %s , host : %s , port : %s , revision : %s]",
                        revisionExportedURLs.size(),
                        serviceInstance.getId(),
                        serviceInstance.getServiceName(),
                        serviceInstance.getHost(),
                        serviceInstance.getPort(),
                        revision
                ));
            }
        }

        return revisionExportedURLs;
    }

    private Map<String, List<URL>> getRevisionExportedURLsMap(String serviceName) {
        return serviceRevisionExportedURLsCache.computeIfAbsent(serviceName, s -> new LinkedHashMap());
    }

    /**
     * Get all services {@link URL URLs} that the specified {@link ServiceInstance service instance} exported from cache
     *
     * @param serviceInstance the {@link ServiceInstance} exports the Dubbo Services
     * @return the same as {@link #getExportedURLs(ServiceInstance)}
     */
    private List<URL> getRevisionExportedURLs(ServiceInstance serviceInstance) {

        if (serviceInstance == null) {
            return emptyList();
        }

        /**
         * 获取serviceInstance对应得serviceName和revision
         */
        String serviceName = serviceInstance.getServiceName();
        // get the revision from the specified {@link ServiceInstance}
        String revision = getExportedServicesRevision(serviceInstance);

        /**
         * 获取缓存中  serviceName和revision对应得服务提供端暴露得服务
         */
        return getRevisionExportedURLs(serviceName, revision);
    }

    /**
     * 获取缓存中  serviceName和revision对应得服务提供端暴露得服务
     * @param serviceName
     * @param revision
     * @return
     */
    private List<URL> getRevisionExportedURLs(String serviceName, String revision) {
        return executeShared(() -> {
            /**
             * 获取缓存中  serviceName和revision对应得服务提供端暴露得服务
             */
            Map<String, List<URL>> revisionExportedURLsMap = getRevisionExportedURLsMap(serviceName);
            List<URL> exportedURLs = revisionExportedURLsMap.get(revision);
            // Get a copy from source in order to prevent the caller trying to change the cached data
            return exportedURLs != null ? new ArrayList<>(exportedURLs) : emptyList();
        });
    }

    /**
     * Get all services {@link URL URLs} that the specified {@link ServiceInstance service instance} exported
     * via the proxy to invoke the {@link MetadataService}
     *
     * @param providerServiceInstance the {@link ServiceInstance} exported the Dubbo services
     * @return The possible result :
     * <ol>
     * <li>The normal result</li>
     * <li>The empty result if the {@link ServiceInstance service instance} did not export yet</li>
     * <li><code>null</code> if there is an invocation error on {@link MetadataService} proxy</li>
     * </ol>
     * @see MetadataServiceProxyFactory
     * @see MetadataService
     */
    private List<URL> getExportedURLs(ServiceInstance providerServiceInstance) {

        List<URL> exportedURLs = null;

        String metadataStorageType = getMetadataStorageType(providerServiceInstance);

        try {
            /**
             * getProxy 生成服务提供端元数据服务得invoker
             */
            MetadataService metadataService = MetadataServiceProxyFactory.getExtension(metadataStorageType)
                    .getProxy(providerServiceInstance);
            if (metadataService != null) {
                /**
                 * 访问服务提供端元数据服务   获取提供端暴露得服务url列表
                 * 访问服务提供端元数据服务   获取提供端暴露得服务url列表
                 * 访问服务提供端元数据服务   获取提供端暴露得服务url列表
                 * dubbo://192.168.50.39:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&default=true&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&methods=sayHello,sayHelloAsync&pid=12896&release=&side=provider&timestamp=1605506790273
                 */
                SortedSet<String> urls = metadataService.getExportedURLs();
                exportedURLs = toURLs(urls);
            }
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(format("Failed to get the exported URLs from the target service instance[%s]",
                        providerServiceInstance), e);
            }
            exportedURLs = null; // set the result to be null if failed to get
        }
        return exportedURLs;
    }

    private void executeExclusively(Runnable runnable) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            runnable.run();
        } finally {
            writeLock.unlock();
        }
    }

    private <T> T executeShared(Supplier<T> supplier) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return supplier.get();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Synthesize new subscribed {@link URL URLs} from old one
     *
     * @param subscribedURL
     * @param serviceInstances
     * @return non-null
     */
    private Collection<? extends URL> synthesizeSubscribedURLs(URL subscribedURL, Collection<ServiceInstance> serviceInstances) {
        return subscribedURLsSynthesizers.stream()
                .filter(synthesizer -> synthesizer.supports(subscribedURL))
                .map(synthesizer -> synthesizer.synthesize(subscribedURL, serviceInstances))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * 1.developer explicitly specifies the application name this interface belongs to
     * 2.check Interface-App mapping
     * 3.use the services specified in registry url.
     *
     * @param subscribedURL
     * @return
     * @throws IllegalStateException If no service name is not found
     */
    protected Set<String> getServices(URL subscribedURL) throws IllegalStateException {

        Set<String> subscribedServices = null;

        /**
         * subscribedURL显式指定此接口所属的应用程序名称
         */
        String serviceNames = subscribedURL.getParameter(PROVIDED_BY);
        if (StringUtils.isNotEmpty(serviceNames)) {
            subscribedServices = parseServices(serviceNames);
        }

        /**
         * 从配置中心中检查url对应的应用程序名称
         */
        if (isEmpty(subscribedServices)) {
            subscribedServices = findMappedServices(subscribedURL);
        }

        /**
         * 从配置中心中检查注册中心url对应的应用程序名称
         */
        if (isEmpty(subscribedServices)) {
            subscribedServices = getSubscribedServices();
        }

        if (isEmpty(subscribedServices)) {
            throw new IllegalStateException("Should has at least one way to know which services this interface belongs to, subscription url: " + subscribedURL);
        }

        return subscribedServices;
    }

    public static Set<String> parseServices(String literalServices) {
        return splitToSet(literalServices, COMMA_SEPARATOR_CHAR, true);
    }

    /**
     * Get the subscribed service names
     *
     * @return non-null
     */
    public Set<String> getSubscribedServices() {
        if (subscribedServices == null) {
            subscribedServices = findMappedServices(getUrl());
        }
        return subscribedServices;
    }

    /**
     * Get the mapped services name by the specified {@link URL}
     *
     * @param url the specified {@link URL}
     * @return empty {@link Set} if not found
     */
    protected Set<String> findMappedServices(URL url) {
        /**
         * 获取url对应的serviceName  CompositeServiceNameMapping
         */
        return serviceNameMapping.get(url);
    }

    /**
     * Create an instance of {@link ServiceDiscoveryRegistry} if supported
     *
     * @param registryURL the {@link URL url} of registry
     * @return <code>null</code> if not supported
     */
    public static ServiceDiscoveryRegistry create(URL registryURL) {
        return supports(registryURL) ? new ServiceDiscoveryRegistry(registryURL) : null;
    }

    /**
     * Supports or not ?
     *
     * @param registryURL the {@link URL url} of registry
     * @return if supported, return <code>true</code>, or <code>false</code>
     */
    public static boolean supports(URL registryURL) {
        return SERVICE_REGISTRY_TYPE.equalsIgnoreCase(registryURL.getParameter(REGISTRY_TYPE_KEY));
    }

    /**
     * 服务消费者可以消费得服务列表
     * 比对  接口名称、version、group、protocol
     * @param subscribedURL
     * @param exportedURLs
     * @return
     */
    private static List<URL> filterSubscribedURLs(URL subscribedURL, List<URL> exportedURLs) {
        return exportedURLs.stream()
                .filter(url -> isSameServiceInterface(subscribedURL, url))
                .filter(url -> isSameParameter(subscribedURL, url, VERSION_KEY))
                .filter(url -> isSameParameter(subscribedURL, url, GROUP_KEY))
                .filter(url -> isCompatibleProtocol(subscribedURL, url))
                .collect(Collectors.toList());
    }

    private static boolean isSameServiceInterface(URL one, URL another) {
        return Objects.equals(one.getServiceInterface(), another.getServiceInterface());
    }

    private static boolean isSameParameter(URL one, URL another, String key) {
        return Objects.equals(one.getParameter(key), another.getParameter(key));
    }

    private static boolean isCompatibleProtocol(URL one, URL another) {
        String protocol = one.getParameter(PROTOCOL_KEY);
        return isCompatibleProtocol(protocol, another);
    }

    private static boolean isCompatibleProtocol(String protocol, URL targetURL) {
        return protocol == null || Objects.equals(protocol, targetURL.getParameter(PROTOCOL_KEY))
                || Objects.equals(protocol, targetURL.getProtocol());
    }
}