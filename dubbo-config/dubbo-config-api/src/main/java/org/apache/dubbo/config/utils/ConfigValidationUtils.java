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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.PropertiesConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConfigCenterConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MetadataReportConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.SslConfig;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.Dispatcher;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.InvokerListener;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.support.MockInvoker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_MONITOR_ADDRESS;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_ALL;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INTERFACE;
import static org.apache.dubbo.common.constants.RegistryConstants.DUBBO_REGISTER_MODE_DEFAULT_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_MODE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.config.Constants.ARCHITECTURE;
import static org.apache.dubbo.config.Constants.CONTEXTPATH_KEY;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.config.Constants.ENVIRONMENT;
import static org.apache.dubbo.config.Constants.IGNORE_CHECK_KEYS;
import static org.apache.dubbo.config.Constants.LAYER_KEY;
import static org.apache.dubbo.config.Constants.NAME;
import static org.apache.dubbo.config.Constants.ORGANIZATION;
import static org.apache.dubbo.config.Constants.OWNER;
import static org.apache.dubbo.config.Constants.STATUS_KEY;
import static org.apache.dubbo.monitor.Constants.LOGSTAT_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_KEY;
import static org.apache.dubbo.registry.Constants.SUBSCRIBE_KEY;
import static org.apache.dubbo.remoting.Constants.CLIENT_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.DISPATCHER_KEY;
import static org.apache.dubbo.remoting.Constants.EXCHANGER_KEY;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.remoting.Constants.TELNET_KEY;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;
import static org.apache.dubbo.rpc.Constants.FAIL_PREFIX;
import static org.apache.dubbo.rpc.Constants.FORCE_PREFIX;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;
import static org.apache.dubbo.rpc.Constants.RETURN_PREFIX;
import static org.apache.dubbo.rpc.Constants.THROW_PREFIX;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class ConfigValidationUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidationUtils.class);
    /**
     * The maximum length of a <b>parameter's value</b>
     */
    private static final int MAX_LENGTH = 200;

    /**
     * The maximum length of a <b>path</b>
     */
    private static final int MAX_PATH_LENGTH = 200;

    /**
     * The rule qualification for <b>name</b>
     */
    private static final Pattern PATTERN_NAME = Pattern.compile("[\\-._0-9a-zA-Z]+");

    /**
     * The rule qualification for <b>multiply name</b>
     */
    private static final Pattern PATTERN_MULTI_NAME = Pattern.compile("[,\\-._0-9a-zA-Z]+");

    /**
     * The rule qualification for <b>method names</b>
     */
    private static final Pattern PATTERN_METHOD_NAME = Pattern.compile("[a-zA-Z][0-9a-zA-Z]*");

    /**
     * The rule qualification for <b>path</b>
     */
    private static final Pattern PATTERN_PATH = Pattern.compile("[/\\-$._0-9a-zA-Z]+");

    /**
     * The pattern matches a value who has a symbol
     */
    private static final Pattern PATTERN_NAME_HAS_SYMBOL = Pattern.compile("[:*,\\s/\\-._0-9a-zA-Z]+");

    /**
     * The pattern matches a property key
     */
    private static final Pattern PATTERN_KEY = Pattern.compile("[*,\\-._0-9a-zA-Z]+");

    public static final String IPV6_START_MARK = "[";

    public static final String IPV6_END_MARK = "]";

    public static List<URL> loadRegistries(AbstractInterfaceConfig interfaceConfig, boolean provider) {
        // check && override if necessary
        List<URL> registryList = new ArrayList<>();
        ApplicationConfig application = interfaceConfig.getApplication();
        List<RegistryConfig> registries = interfaceConfig.getRegistries();
        if (CollectionUtils.isNotEmpty(registries)) {
            for (RegistryConfig config : registries) {
                String address = config.getAddress();
                if (StringUtils.isEmpty(address)) {
                    address = ANYHOST_VALUE;
                }
                if (!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<String, String>();
                    AbstractConfig.appendParameters(map, application);
                    AbstractConfig.appendParameters(map, config);
                    map.put(PATH_KEY, RegistryService.class.getName());
                    AbstractInterfaceConfig.appendRuntimeParameters(map);
                    if (!map.containsKey(PROTOCOL_KEY)) {
                        map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);

                    for (URL url : urls) {
                        url = URLBuilder.from(url)
                            .addParameter(REGISTRY_KEY, url.getProtocol())
                            .setProtocol(extractRegistryType(url))
                            .setScopeModel(interfaceConfig.getScopeModel())
                            .build();
                        if ((provider && url.getParameter(REGISTER_KEY, true))
                            || (!provider && url.getParameter(SUBSCRIBE_KEY, true))) {
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        return genCompatibleRegistries(interfaceConfig.getScopeModel(), registryList, provider);
    }

    private static List<URL> genCompatibleRegistries(ScopeModel scopeModel, List<URL> registryList, boolean provider) {
        List<URL> result = new ArrayList<>(registryList.size());
        registryList.forEach(registryURL -> {
            if (provider) {
                // for registries enabled service discovery, automatically register interface compatible addresses.
                String registerMode;
                if (SERVICE_REGISTRY_PROTOCOL.equals(registryURL.getProtocol())) {
                    registerMode = registryURL.getParameter(REGISTER_MODE_KEY, ConfigurationUtils.getCachedDynamicProperty(scopeModel, DUBBO_REGISTER_MODE_DEFAULT_KEY, DEFAULT_REGISTER_MODE_INSTANCE));
                    if (!isValidRegisterMode(registerMode)) {
                        registerMode = DEFAULT_REGISTER_MODE_INSTANCE;
                    }
                    result.add(registryURL);
                    if (DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode)
                        && registryNotExists(registryURL, registryList, REGISTRY_PROTOCOL)) {
                        URL interfaceCompatibleRegistryURL = URLBuilder.from(registryURL)
                            .setProtocol(REGISTRY_PROTOCOL)
                            .removeParameter(REGISTRY_TYPE_KEY)
                            .build();
                        result.add(interfaceCompatibleRegistryURL);
                    }
                } else {
                    registerMode = registryURL.getParameter(REGISTER_MODE_KEY, ConfigurationUtils.getCachedDynamicProperty(scopeModel, DUBBO_REGISTER_MODE_DEFAULT_KEY, DEFAULT_REGISTER_MODE_ALL));
                    if (!isValidRegisterMode(registerMode)) {
                        registerMode = DEFAULT_REGISTER_MODE_INTERFACE;
                    }
                    if ((DEFAULT_REGISTER_MODE_INSTANCE.equalsIgnoreCase(registerMode) || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode))
                        && registryNotExists(registryURL, registryList, SERVICE_REGISTRY_PROTOCOL)) {
                        URL serviceDiscoveryRegistryURL = URLBuilder.from(registryURL)
                            .setProtocol(SERVICE_REGISTRY_PROTOCOL)
                            .removeParameter(REGISTRY_TYPE_KEY)
                            .build();
                        result.add(serviceDiscoveryRegistryURL);
                    }

                    if (DEFAULT_REGISTER_MODE_INTERFACE.equalsIgnoreCase(registerMode) || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode)) {
                        result.add(registryURL);
                    }
                }

                FrameworkStatusReportService reportService = ScopeModelUtil.getApplicationModel(scopeModel).getBeanFactory().getBean(FrameworkStatusReportService.class);
                reportService.reportRegistrationStatus(reportService.createRegistrationReport(registerMode));
            } else {
                result.add(registryURL);
            }
        });

        return result;
    }

    private static boolean isValidRegisterMode(String mode) {
        return StringUtils.isNotEmpty(mode)
            && (DEFAULT_REGISTER_MODE_INTERFACE.equalsIgnoreCase(mode)
            || DEFAULT_REGISTER_MODE_INSTANCE.equalsIgnoreCase(mode)
            || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(mode)
        );
    }

    private static boolean registryNotExists(URL registryURL, List<URL> registryList, String registryType) {
        return registryList.stream().noneMatch(
            url -> registryType.equals(url.getProtocol()) && registryURL.getBackupAddress().equals(url.getBackupAddress())
        );
    }

    public static URL loadMonitor(AbstractInterfaceConfig interfaceConfig, URL registryURL) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(INTERFACE_KEY, MonitorService.class.getName());
        AbstractInterfaceConfig.appendRuntimeParameters(map);
        //set ip
        String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)) {
            hostToRegistry = NetUtils.getLocalHost();
        } else if (NetUtils.isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" +
                DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(REGISTER_IP_KEY, hostToRegistry);

        MonitorConfig monitor = interfaceConfig.getMonitor();
        ApplicationConfig application = interfaceConfig.getApplication();
        AbstractConfig.appendParameters(map, monitor);
        AbstractConfig.appendParameters(map, application);
        String address = null;
        String sysAddress = System.getProperty(DUBBO_MONITOR_ADDRESS);
        if (sysAddress != null && sysAddress.length() > 0) {
            address = sysAddress;
        } else if (monitor != null) {
            address = monitor.getAddress();
        }
        String protocol = monitor == null ? null : monitor.getProtocol();
        if (monitor != null &&
            (REGISTRY_PROTOCOL.equals(protocol) || SERVICE_REGISTRY_PROTOCOL.equals(protocol))
            && registryURL != null) {
            return URLBuilder.from(registryURL)
                .setProtocol(DUBBO_PROTOCOL)
                .addParameter(PROTOCOL_KEY, protocol)
                .putAttribute(REFER_KEY, map)
                .build();
        } else if (ConfigUtils.isNotEmpty(address) || ConfigUtils.isNotEmpty(protocol)) {
            if (!map.containsKey(PROTOCOL_KEY)) {
                if (interfaceConfig.getScopeModel().getExtensionLoader(MonitorFactory.class).hasExtension(LOGSTAT_PROTOCOL)) {
                    map.put(PROTOCOL_KEY, LOGSTAT_PROTOCOL);
                } else if (ConfigUtils.isNotEmpty(protocol)) {
                    map.put(PROTOCOL_KEY, protocol);
                } else {
                    map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                }
            }
            if (ConfigUtils.isEmpty(address)) {
                address = LOCALHOST_VALUE;
            }
            return UrlUtils.parseURL(address, map);
        }
        return null;
    }

    /**
     * Legitimacy check and setup of local simulated operations. The operations can be a string with Simple operation or
     * a classname whose {@link Class} implements a particular function
     *
     * @param interfaceClass for provider side, it is the {@link Class} of the service that will be exported; for consumer
     *                       side, it is the {@link Class} of the remote service interface that will be referenced
     */
    public static void checkMock(Class<?> interfaceClass, AbstractInterfaceConfig config) {
        String mock = config.getMock();
        if (ConfigUtils.isEmpty(mock)) {
            return;
        }

        String normalizedMock = MockInvoker.normalizeMock(mock);
        if (normalizedMock.startsWith(RETURN_PREFIX)) {
            normalizedMock = normalizedMock.substring(RETURN_PREFIX.length()).trim();
            try {
                //Check whether the mock value is legal, if it is illegal, throw exception
                MockInvoker.parseMockValue(normalizedMock);
            } catch (Exception e) {
                throw new IllegalStateException("Illegal mock return in <dubbo:service/reference ... " +
                    "mock=\"" + mock + "\" />");
            }
        } else if (normalizedMock.startsWith(THROW_PREFIX)) {
            normalizedMock = normalizedMock.substring(THROW_PREFIX.length()).trim();
            if (ConfigUtils.isNotEmpty(normalizedMock)) {
                try {
                    //Check whether the mock value is legal
                    MockInvoker.getThrowable(normalizedMock);
                } catch (Exception e) {
                    throw new IllegalStateException("Illegal mock throw in <dubbo:service/reference ... " +
                        "mock=\"" + mock + "\" />");
                }
            }
        } else {
            //Check whether the mock class is a implementation of the interfaceClass, and if it has a default constructor
            MockInvoker.getMockObject(config.getScopeModel().getExtensionDirector(), normalizedMock, interfaceClass);
        }
    }

    public static void validateAbstractInterfaceConfig(AbstractInterfaceConfig config) {
        checkName(LOCAL_KEY, config.getLocal());
        checkName("stub", config.getStub());
        checkMultiName("owner", config.getOwner());

        checkExtension(config.getScopeModel(), ProxyFactory.class, PROXY_KEY, config.getProxy());
        checkExtension(config.getScopeModel(), Cluster.class, CLUSTER_KEY, config.getCluster());
        checkMultiExtension(config.getScopeModel(), Arrays.asList(Filter.class, ClusterFilter.class), FILTER_KEY, config.getFilter());
        checkNameHasSymbol(LAYER_KEY, config.getLayer());

        List<MethodConfig> methods = config.getMethods();
        if (CollectionUtils.isNotEmpty(methods)) {
            methods.forEach(ConfigValidationUtils::validateMethodConfig);
        }
    }

    public static void validateServiceConfig(ServiceConfig config) {
        checkKey(VERSION_KEY, config.getVersion());
        checkKey(GROUP_KEY, config.getGroup());
        checkName(TOKEN_KEY, config.getToken());
        checkPathName(PATH_KEY, config.getPath());

        checkMultiExtension(config.getScopeModel(), ExporterListener.class, "listener", config.getListener());

        validateAbstractInterfaceConfig(config);

        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                validateRegistryConfig(registry);
            }
        }

        List<ProtocolConfig> protocols = config.getProtocols();
        if (protocols != null) {
            for (ProtocolConfig protocol : protocols) {
                validateProtocolConfig(protocol);
            }
        }

        ProviderConfig providerConfig = config.getProvider();
        if (providerConfig != null) {
            validateProviderConfig(providerConfig);
        }
    }

    public static void validateReferenceConfig(ReferenceConfig config) {
        checkMultiExtension(config.getScopeModel(), InvokerListener.class, "listener", config.getListener());
        checkKey(VERSION_KEY, config.getVersion());
        checkKey(GROUP_KEY, config.getGroup());
        checkName(CLIENT_KEY, config.getClient());

        validateAbstractInterfaceConfig(config);

        List<RegistryConfig> registries = config.getRegistries();
        if (registries != null) {
            for (RegistryConfig registry : registries) {
                validateRegistryConfig(registry);
            }
        }

        ConsumerConfig consumerConfig = config.getConsumer();
        if (consumerConfig != null) {
            validateConsumerConfig(consumerConfig);
        }
    }

    public static void validateConfigCenterConfig(ConfigCenterConfig config) {
        if (config != null) {
            checkParameterName(config.getParameters());
        }
    }

    public static void validateApplicationConfig(ApplicationConfig config) {
        if (config == null) {
            return;
        }

        if (!config.isValid()) {
            throw new IllegalStateException("No application config found or it's not a valid config! " +
                "Please add <dubbo:application name=\"...\" /> to your spring config.");
        }

        // backward compatibility
        ScopeModel scopeModel = ScopeModelUtil.getOrDefaultApplicationModel(config.getScopeModel());
        PropertiesConfiguration configuration = scopeModel.getModelEnvironment().getPropertiesConfiguration();
        String wait = configuration.getProperty(SHUTDOWN_WAIT_KEY);
        if (wait != null && wait.trim().length() > 0) {
            System.setProperty(SHUTDOWN_WAIT_KEY, wait.trim());
        } else {
            wait = configuration.getProperty(SHUTDOWN_WAIT_SECONDS_KEY);
            if (wait != null && wait.trim().length() > 0) {
                System.setProperty(SHUTDOWN_WAIT_SECONDS_KEY, wait.trim());
            }
        }

        checkName(NAME, config.getName());
        checkMultiName(OWNER, config.getOwner());
        checkName(ORGANIZATION, config.getOrganization());
        checkName(ARCHITECTURE, config.getArchitecture());
        checkName(ENVIRONMENT, config.getEnvironment());
        checkParameterName(config.getParameters());
    }

    public static void validateModuleConfig(ModuleConfig config) {
        if (config != null) {
            checkName(NAME, config.getName());
            checkName(OWNER, config.getOwner());
            checkName(ORGANIZATION, config.getOrganization());
        }
    }

    public static void validateMetadataConfig(MetadataReportConfig metadataReportConfig) {
        if (metadataReportConfig == null) {
            return;
        }
    }

    public static void validateMetricsConfig(MetricsConfig metricsConfig) {
        if (metricsConfig == null) {
            return;
        }
    }

    public static void validateSslConfig(SslConfig sslConfig) {
        if (sslConfig == null) {
            return;
        }
    }

    public static void validateMonitorConfig(MonitorConfig config) {
        if (config != null) {
            if (!config.isValid()) {
                logger.info("There's no valid monitor config found, if you want to open monitor statistics for Dubbo, " +
                    "please make sure your monitor is configured properly.");
            }

            checkParameterName(config.getParameters());
        }
    }

    public static void validateProtocolConfig(ProtocolConfig config) {
        if (config != null) {
            String name = config.getName();
            checkName("name", name);
            checkHost(HOST_KEY, config.getHost());
            checkPathName("contextpath", config.getContextpath());


            if (DUBBO_PROTOCOL.equals(name)) {
                checkMultiExtension(config.getScopeModel(), Codec2.class, CODEC_KEY, config.getCodec());
                checkMultiExtension(config.getScopeModel(), Serialization.class, SERIALIZATION_KEY, config.getSerialization());
                checkMultiExtension(config.getScopeModel(), Transporter.class, SERVER_KEY, config.getServer());
                checkMultiExtension(config.getScopeModel(), Transporter.class, CLIENT_KEY, config.getClient());
            }

            checkMultiExtension(config.getScopeModel(), TelnetHandler.class, TELNET_KEY, config.getTelnet());
            checkMultiExtension(config.getScopeModel(), StatusChecker.class, "status", config.getStatus());
            checkExtension(config.getScopeModel(), Transporter.class, TRANSPORTER_KEY, config.getTransporter());
            checkExtension(config.getScopeModel(), Exchanger.class, EXCHANGER_KEY, config.getExchanger());
            checkExtension(config.getScopeModel(), Dispatcher.class, DISPATCHER_KEY, config.getDispatcher());
            checkExtension(config.getScopeModel(), Dispatcher.class, "dispather", config.getDispather());
            checkExtension(config.getScopeModel(), ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        }
    }

    public static void validateProviderConfig(ProviderConfig config) {
        checkPathName(CONTEXTPATH_KEY, config.getContextpath());
        checkExtension(config.getScopeModel(), ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        checkMultiExtension(config.getScopeModel(), TelnetHandler.class, TELNET_KEY, config.getTelnet());
        checkMultiExtension(config.getScopeModel(), StatusChecker.class, STATUS_KEY, config.getStatus());
        checkExtension(config.getScopeModel(), Transporter.class, TRANSPORTER_KEY, config.getTransporter());
        checkExtension(config.getScopeModel(), Exchanger.class, EXCHANGER_KEY, config.getExchanger());
    }

    public static void validateConsumerConfig(ConsumerConfig config) {
        if (config == null) {
            return;
        }
    }

    public static void validateRegistryConfig(RegistryConfig config) {
        checkName(PROTOCOL_KEY, config.getProtocol());
        checkName(USERNAME_KEY, config.getUsername());
        checkLength(PASSWORD_KEY, config.getPassword());
        checkPathLength(FILE_KEY, config.getFile());
        checkName(TRANSPORTER_KEY, config.getTransporter());
        checkName(SERVER_KEY, config.getServer());
        checkName(CLIENT_KEY, config.getClient());
        checkParameterName(config.getParameters());
    }

    public static void validateMethodConfig(MethodConfig config) {
        checkExtension(config.getScopeModel(), LoadBalance.class, LOADBALANCE_KEY, config.getLoadbalance());
        checkParameterName(config.getParameters());
        checkMethodName("name", config.getName());

        String mock = config.getMock();
        if (StringUtils.isNotEmpty(mock)) {
            if (mock.startsWith(RETURN_PREFIX) || mock.startsWith(THROW_PREFIX + " ")) {
                checkLength(MOCK_KEY, mock);
            } else if (mock.startsWith(FAIL_PREFIX) || mock.startsWith(FORCE_PREFIX)) {
                checkNameHasSymbol(MOCK_KEY, mock);
            } else {
                checkName(MOCK_KEY, mock);
            }
        }
    }

    private static String extractRegistryType(URL url) {
        return UrlUtils.hasServiceDiscoveryRegistryTypeKey(url) ? SERVICE_REGISTRY_PROTOCOL : getRegistryProtocolType(url);
    }

    private static String getRegistryProtocolType(URL url) {
        String registryProtocol = url.getParameter("registry-protocol-type");
        return StringUtils.isNotEmpty(registryProtocol) ? registryProtocol : REGISTRY_PROTOCOL;
    }

    public static void checkExtension(ScopeModel scopeModel, Class<?> type, String property, String value) {
        checkName(property, value);
        if (StringUtils.isNotEmpty(value)
            && !scopeModel.getExtensionLoader(type).hasExtension(value)) {
            throw new IllegalStateException("No such extension " + value + " for " + property + "/" + type.getName());
        }
    }

    /**
     * Check whether there is a <code>Extension</code> who's name (property) is <code>value</code> (special treatment is
     * required)
     *
     * @param type     The Extension type
     * @param property The extension key
     * @param value    The Extension name
     */
    public static void checkMultiExtension(ScopeModel scopeModel, Class<?> type, String property, String value) {
        checkMultiExtension(scopeModel,Collections.singletonList(type), property, value);
    }

    public static void checkMultiExtension(ScopeModel scopeModel, List<Class<?>> types, String property, String value) {
        checkMultiName(property, value);
        if (StringUtils.isNotEmpty(value)) {
            String[] values = value.split("\\s*[,]+\\s*");
            for (String v : values) {
                if (v.startsWith(REMOVE_VALUE_PREFIX)) {
                    v = v.substring(1);
                }
                if (DEFAULT_KEY.equals(v)) {
                    continue;
                }
                boolean match = false;
                for (Class<?> type : types) {
                    if (scopeModel.getExtensionLoader(type).hasExtension(v)) {
                        match = true;
                    }
                }
                if (!match) {
                    throw new IllegalStateException("No such extension " + v + " for " + property + "/" +
                        types.stream().map(Class::getName).collect(Collectors.joining(",")));
                }
            }
        }
    }

    public static void checkLength(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, null);
    }

    public static void checkPathLength(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, null);
    }

    public static void checkName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME);
    }

    public static void checkHost(String property, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (value.startsWith(IPV6_START_MARK) && value.endsWith(IPV6_END_MARK)) {
            // if the value start with "[" and end with "]", check whether it is IPV6
            try {
                InetAddress.getByName(value);
                return;
            } catch (UnknownHostException e) {
                // not a IPv6 string, do nothing, go on to checkName
            }
        }
        checkName(property, value);
    }

    public static void checkNameHasSymbol(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_NAME_HAS_SYMBOL);
    }

    public static void checkKey(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_KEY);
    }

    public static void checkMultiName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_MULTI_NAME);
    }

    public static void checkPathName(String property, String value) {
        checkProperty(property, value, MAX_PATH_LENGTH, PATTERN_PATH);
    }

    public static void checkMethodName(String property, String value) {
        checkProperty(property, value, MAX_LENGTH, PATTERN_METHOD_NAME);
    }

    public static void checkParameterName(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        List<String> ignoreCheckKeys = new ArrayList<>();
        ignoreCheckKeys.add(BACKUP_KEY);
        String ignoreCheckKeysStr = parameters.get(IGNORE_CHECK_KEYS);
        if (!StringUtils.isBlank(ignoreCheckKeysStr)) {
            ignoreCheckKeys.addAll(Arrays.asList(ignoreCheckKeysStr.split(",")));
        }
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (!ignoreCheckKeys.contains(entry.getKey())) {
                checkNameHasSymbol(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void checkProperty(String property, String value, int maxlength, Pattern pattern) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (value.length() > maxlength) {
            logger.error("Invalid " + property + "=\"" + value + "\" is longer than " + maxlength);
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(value);
            if (!matcher.matches()) {
                logger.error("Invalid " + property + "=\"" + value + "\" contains illegal " +
                    "character, only digit, letter, '-', '_' or '.' is legal.");
            }
        }
    }

}
