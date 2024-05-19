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
package org.apache.dubbo.config;

import org.apache.dubbo.common.aot.NativeDetector;
import org.apache.dubbo.common.compiler.support.AdaptiveCompiler;
import org.apache.dubbo.common.infra.InfraAdapter;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_ENABLE;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE;
import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_ISOLATION;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LIVENESS_PROBE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.READINESS_PROBE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_LOCAL_FILE_CACHE_ENABLED;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.STARTUP_PROBE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_WHITELIST;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_WHITELIST_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.ANONYMOUS_ACCESS_ALLOW_COMMANDS;
import static org.apache.dubbo.common.constants.QosConstants.ANONYMOUS_ACCESS_PERMISSION_LEVEL;
import static org.apache.dubbo.common.constants.QosConstants.ANONYMOUS_ACCESS_PERMISSION_LEVEL_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_CHECK;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT_COMPATIBLE;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_MODE_KEY;
import static org.apache.dubbo.config.Constants.DEFAULT_APP_NAME;
import static org.apache.dubbo.config.Constants.DEFAULT_NATIVE_COMPILER;
import static org.apache.dubbo.config.Constants.DEVELOPMENT_ENVIRONMENT;
import static org.apache.dubbo.config.Constants.PRODUCTION_ENVIRONMENT;
import static org.apache.dubbo.config.Constants.TEST_ENVIRONMENT;

/**
 * Configuration for the dubbo application.
 *
 * @export
 */
public class ApplicationConfig extends AbstractConfig {

    private static final long serialVersionUID = 5508512956753757169L;

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ApplicationConfig.class);

    /**
     * The Application name.
     */
    private String name;

    /**
     * The application version.
     */
    private String version;

    /**
     * The application owner.
     */
    private String owner;

    /**
     * The application's organization (BU).
     */
    private String organization;

    /**
     * Architecture layer.
     */
    private String architecture;

    /**
     * Environment, e.g., dev, test, or production.
     */
    private String environment;

    /**
     * Java compiler.
     */
    private String compiler;

    /**
     * The type of log access.
     */
    private String logger;

    /**
     * Registry centers.
     */
    private List<RegistryConfig> registries;

    /**
     * The comma-separated list of registry IDs to which the service will be registered.
     */
    private String registryIds;

    /**
     * Monitor center.
     */
    private MonitorConfig monitor;

    /**
     * Directory for saving thread dump.
     */
    private String dumpDirectory;

    /**
     * Whether to enable saving thread dump or not.
     */
    private Boolean dumpEnable;

    /**
     * Whether to enable Quality of Service (QoS) or not.
     */
    private Boolean qosEnable;

    /**
     * Whether QoS should start successfully or not, will check qosEnable first.
     */
    private Boolean qosCheck;

    /**
     * The QoS host to listen.
     */
    private String qosHost;

    /**
     * The QoS port to listen.
     */
    private Integer qosPort;

    /**
     * Should we accept foreign IP or not?
     */
    private Boolean qosAcceptForeignIp;

    /**
     * When we disable accepting foreign IP, support specifying foreign IPs in the whitelist.
     */
    private String qosAcceptForeignIpWhitelist;

    /**
     * The anonymous (any foreign IP) access permission level, default is NONE, which means no access to any command.
     */
    private String qosAnonymousAccessPermissionLevel;

    /**
     * The anonymous (any foreign IP) allowed commands, default is empty, which means no access to any command.
     */
    private String qosAnonymousAllowCommands;

    /**
     * Customized parameters.
     */
    private Map<String, String> parameters;

    /**
     * Config the shutdown wait.
     */
    private String shutwait;

    /**
     * Hostname.
     */
    private String hostname;

    /**
     * Metadata type, local or remote. If 'remote' is chosen, you need to specify a metadata center further.
     */
    private String metadataType;

    /**
     * Used to control whether to register the instance with the registry or not. Set to 'false' only when the instance is a pure consumer.
     */
    private Boolean registerConsumer;

    /**
     * Repository.
     */
    private String repository;

    /**
     * Whether to enable file caching.
     */
    private Boolean enableFileCache;

    /**
     * The preferred protocol (name) of this application, convenient for places where it's hard to determine the preferred protocol.
     */
    private String protocol;

    /**
     * The protocol used for peer-to-peer metadata transmission.
     */
    private String metadataServiceProtocol;

    /**
     * Metadata Service, used in Service Discovery.
     */
    private Integer metadataServicePort;

    /**
     * The retry interval of service name mapping.
     */
    private Integer mappingRetryInterval;

    /**
     * Used to set extensions of the probe in QoS.
     */
    private String livenessProbe;

    /**
     * The probe for checking the readiness of the application.
     */
    private String readinessProbe;

    /**
     * The probe for checking the startup of the application.
     */
    private String startupProbe;

    /**
     * Register mode.
     */
    private String registerMode;

    /**
     * Whether to enable protection against empty objects.
     */
    private Boolean enableEmptyProtection;

    /**
     * The status of class serialization checking.
     */
    private String serializeCheckStatus;

    /**
     * Whether to automatically trust serialized classes.
     */
    private Boolean autoTrustSerializeClass;

    /**
     * The trust level for serialized classes.
     */
    private Integer trustSerializeClassLevel;

    /**
     * Whether to check serializable.
     */
    private Boolean checkSerializable;

    /**
     * Thread pool management mode: 'default' or 'isolation'.
     */
    private String executorManagementMode;

    /**
     * Only use the new version of metadataService (MetadataServiceV2).
     * <br> MetadataServiceV2 have better compatibility with other language's dubbo implement (dubbo-go).
     * <br> If set to false (default):
     * <br>  1. If your services are using triple protocol and {@link #metadataServiceProtocol} is not set
     * <br>     - Dubbo will export both MetadataService and MetadataServiceV2 with triple
     * <br>  2. Set {@link #metadataServiceProtocol} = tri
     * <br>     - Dubbo will export both MetadataService and MetadataServiceV2 with triple
     * <br>  3. Set {@link #metadataServiceProtocol} != tri
     * <br>     - Dubbo will only export MetadataService
     * <br>  4. Your services are not using triple protocol, and {@link #metadataServiceProtocol} is not set
     * <br>     - Dubbo will only export MetadataService
     * <br>
     * <br>  If set to true, dubbo will try to only use MetadataServiceV2.
     * <br>  It only activates when meet at least one of the following cases:
     * <br>     1. Manually set {@link #metadataServiceProtocol} = tri
     * <br>     2. Your services are using triple protocol
     * <br>
     */
    private Boolean onlyUseMetadataV2;

    public ApplicationConfig() {}

    public ApplicationConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    public ApplicationConfig(String name) {
        setName(name);
    }

    public ApplicationConfig(ApplicationModel applicationModel, String name) {
        super(applicationModel);
        setName(name);
    }

    @Override
    protected void checkDefault() {
        super.checkDefault();
        if (protocol == null) {
            protocol = DUBBO;
        }
        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOGGER.warn(COMMON_UNEXPECTED_EXCEPTION, "", "", "Failed to get the hostname of current instance.", e);
                hostname = "UNKNOWN";
            }
        }
        if (executorManagementMode == null) {
            executorManagementMode = EXECUTOR_MANAGEMENT_MODE_ISOLATION;
        }
        if (enableFileCache == null) {
            enableFileCache = Boolean.TRUE;
        }
    }

    @Parameter(key = APPLICATION_KEY, required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Parameter(key = APPLICATION_VERSION_KEY)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        if (environment != null
                && !(DEVELOPMENT_ENVIRONMENT.equals(environment)
                        || TEST_ENVIRONMENT.equals(environment)
                        || PRODUCTION_ENVIRONMENT.equals(environment))) {

            throw new IllegalStateException(String.format(
                    "Unsupported environment: %s, only support %s/%s/%s, default is %s.",
                    environment,
                    DEVELOPMENT_ENVIRONMENT,
                    TEST_ENVIRONMENT,
                    PRODUCTION_ENVIRONMENT,
                    PRODUCTION_ENVIRONMENT));
        }
        this.environment = environment;
    }

    public RegistryConfig getRegistry() {
        return CollectionUtils.isEmpty(registries) ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<>(1);
        registries.add(registry);
        this.registries = registries;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({"unchecked"})
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>) registries;
    }

    @Parameter(excluded = true)
    public String getRegistryIds() {
        return registryIds;
    }

    public void setRegistryIds(String registryIds) {
        this.registryIds = registryIds;
    }

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public String getCompiler() {
        if (NativeDetector.inNativeImage()) {
            return DEFAULT_NATIVE_COMPILER;
        } else {
            return compiler;
        }
    }

    public void setCompiler(String compiler) {

        if (NativeDetector.inNativeImage()) {
            this.compiler = DEFAULT_NATIVE_COMPILER;
            AdaptiveCompiler.setDefaultCompiler(DEFAULT_NATIVE_COMPILER);
        } else {
            this.compiler = compiler;
            AdaptiveCompiler.setDefaultCompiler(compiler);
        }
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
        LoggerFactory.setLoggerAdapter(getApplicationModel().getFrameworkModel(), logger);
    }

    @Parameter(key = DUMP_DIRECTORY)
    public String getDumpDirectory() {
        return dumpDirectory;
    }

    public void setDumpDirectory(String dumpDirectory) {
        this.dumpDirectory = dumpDirectory;
    }

    @Parameter(key = DUMP_ENABLE)
    public Boolean getDumpEnable() {
        return dumpEnable;
    }

    public void setDumpEnable(Boolean dumpEnable) {
        this.dumpEnable = dumpEnable;
    }

    @Parameter(key = QOS_ENABLE)
    public Boolean getQosEnable() {
        return qosEnable;
    }

    public void setQosEnable(Boolean qosEnable) {
        this.qosEnable = qosEnable;
    }

    @Parameter(key = QOS_CHECK)
    public Boolean getQosCheck() {
        return qosCheck;
    }

    public void setQosCheck(Boolean qosCheck) {
        this.qosCheck = qosCheck;
    }

    @Parameter(key = QOS_HOST)
    public String getQosHost() {
        return qosHost;
    }

    public void setQosHost(String qosHost) {
        this.qosHost = qosHost;
    }

    @Parameter(key = QOS_PORT)
    public Integer getQosPort() {
        return qosPort;
    }

    public void setQosPort(Integer qosPort) {
        this.qosPort = qosPort;
    }

    @Parameter(key = ACCEPT_FOREIGN_IP)
    public Boolean getQosAcceptForeignIp() {
        return qosAcceptForeignIp;
    }

    public void setQosAcceptForeignIp(Boolean qosAcceptForeignIp) {
        this.qosAcceptForeignIp = qosAcceptForeignIp;
    }

    @Parameter(key = ACCEPT_FOREIGN_IP_WHITELIST)
    public String getQosAcceptForeignIpWhitelist() {
        return qosAcceptForeignIpWhitelist;
    }

    public void setQosAcceptForeignIpWhitelist(String qosAcceptForeignIpWhitelist) {
        this.qosAcceptForeignIpWhitelist = qosAcceptForeignIpWhitelist;
    }

    @Parameter(key = ANONYMOUS_ACCESS_PERMISSION_LEVEL)
    public String getQosAnonymousAccessPermissionLevel() {
        return qosAnonymousAccessPermissionLevel;
    }

    public void setQosAnonymousAccessPermissionLevel(String qosAnonymousAccessPermissionLevel) {
        this.qosAnonymousAccessPermissionLevel = qosAnonymousAccessPermissionLevel;
    }

    @Parameter(key = ANONYMOUS_ACCESS_ALLOW_COMMANDS)
    public String getQosAnonymousAllowCommands() {
        return qosAnonymousAllowCommands;
    }

    public void setQosAnonymousAllowCommands(String qosAnonymousAllowCommands) {
        this.qosAnonymousAllowCommands = qosAnonymousAllowCommands;
    }

    /**
     * The format is the same as the springboot, including: getQosEnableCompatible(), getQosPortCompatible(), getQosAcceptForeignIpCompatible().
     *
     */
    @Parameter(key = QOS_ENABLE_COMPATIBLE, excluded = true, attribute = false)
    public Boolean getQosEnableCompatible() {
        return getQosEnable();
    }

    public void setQosEnableCompatible(Boolean qosEnable) {
        setQosEnable(qosEnable);
    }

    @Parameter(key = QOS_HOST_COMPATIBLE, excluded = true, attribute = false)
    public String getQosHostCompatible() {
        return getQosHost();
    }

    public void setQosHostCompatible(String qosHost) {
        this.setQosHost(qosHost);
    }

    @Parameter(key = QOS_PORT_COMPATIBLE, excluded = true, attribute = false)
    public Integer getQosPortCompatible() {
        return getQosPort();
    }

    public void setQosPortCompatible(Integer qosPort) {
        this.setQosPort(qosPort);
    }

    @Parameter(key = ACCEPT_FOREIGN_IP_COMPATIBLE, excluded = true, attribute = false)
    public Boolean getQosAcceptForeignIpCompatible() {
        return this.getQosAcceptForeignIp();
    }

    public void setQosAcceptForeignIpCompatible(Boolean qosAcceptForeignIp) {
        this.setQosAcceptForeignIp(qosAcceptForeignIp);
    }

    @Parameter(key = ACCEPT_FOREIGN_IP_WHITELIST_COMPATIBLE, excluded = true, attribute = false)
    public String getQosAcceptForeignIpWhitelistCompatible() {
        return this.getQosAcceptForeignIpWhitelist();
    }

    public void setQosAcceptForeignIpWhitelistCompatible(String qosAcceptForeignIpWhitelist) {
        this.setQosAcceptForeignIpWhitelist(qosAcceptForeignIpWhitelist);
    }

    @Parameter(key = ANONYMOUS_ACCESS_PERMISSION_LEVEL_COMPATIBLE, excluded = true, attribute = false)
    public String getQosAnonymousAccessPermissionLevelCompatible() {
        return this.getQosAnonymousAccessPermissionLevel();
    }

    public void setQosAnonymousAccessPermissionLevelCompatible(String qosAnonymousAccessPermissionLevel) {
        this.setQosAnonymousAccessPermissionLevel(qosAnonymousAccessPermissionLevel);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getShutwait() {
        return shutwait;
    }

    public void setShutwait(String shutwait) {
        System.setProperty(SHUTDOWN_WAIT_KEY, shutwait);
        this.shutwait = shutwait;
    }

    @Parameter(excluded = true)
    public String getHostname() {
        return hostname;
    }

    @Override
    @Parameter(excluded = true, attribute = false)
    public boolean isValid() {
        return !StringUtils.isEmpty(name);
    }

    @Parameter(key = METADATA_KEY)
    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }

    public Boolean getRegisterConsumer() {
        return registerConsumer;
    }

    public void setRegisterConsumer(Boolean registerConsumer) {
        this.registerConsumer = registerConsumer;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    @Parameter(key = REGISTRY_LOCAL_FILE_CACHE_ENABLED)
    public Boolean getEnableFileCache() {
        return enableFileCache;
    }

    public void setEnableFileCache(Boolean enableFileCache) {
        this.enableFileCache = enableFileCache;
    }

    @Parameter(key = REGISTER_MODE_KEY)
    public String getRegisterMode() {
        return registerMode;
    }

    public void setRegisterMode(String registerMode) {
        this.registerMode = registerMode;
    }

    @Parameter(key = ENABLE_EMPTY_PROTECTION_KEY)
    public Boolean getEnableEmptyProtection() {
        return enableEmptyProtection;
    }

    public void setEnableEmptyProtection(Boolean enableEmptyProtection) {
        this.enableEmptyProtection = enableEmptyProtection;
    }

    @Parameter(excluded = true, key = APPLICATION_PROTOCOL_KEY)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Parameter(key = METADATA_SERVICE_PORT_KEY)
    public Integer getMetadataServicePort() {
        return metadataServicePort;
    }

    public void setMetadataServicePort(Integer metadataServicePort) {
        this.metadataServicePort = metadataServicePort;
    }

    public Integer getMappingRetryInterval() {
        return mappingRetryInterval;
    }

    public void setMappingRetryInterval(Integer mappingRetryInterval) {
        this.mappingRetryInterval = mappingRetryInterval;
    }

    @Parameter(key = METADATA_SERVICE_PROTOCOL_KEY)
    public String getMetadataServiceProtocol() {
        return metadataServiceProtocol;
    }

    public void setMetadataServiceProtocol(String metadataServiceProtocol) {
        this.metadataServiceProtocol = metadataServiceProtocol;
    }

    @Parameter(key = LIVENESS_PROBE_KEY)
    public String getLivenessProbe() {
        return livenessProbe;
    }

    public void setLivenessProbe(String livenessProbe) {
        this.livenessProbe = livenessProbe;
    }

    @Parameter(key = READINESS_PROBE_KEY)
    public String getReadinessProbe() {
        return readinessProbe;
    }

    public void setReadinessProbe(String readinessProbe) {
        this.readinessProbe = readinessProbe;
    }

    @Parameter(key = STARTUP_PROBE)
    public String getStartupProbe() {
        return startupProbe;
    }

    public void setStartupProbe(String startupProbe) {
        this.startupProbe = startupProbe;
    }

    public String getSerializeCheckStatus() {
        return serializeCheckStatus;
    }

    public void setSerializeCheckStatus(String serializeCheckStatus) {
        this.serializeCheckStatus = serializeCheckStatus;
    }

    public Boolean getAutoTrustSerializeClass() {
        return autoTrustSerializeClass;
    }

    public void setAutoTrustSerializeClass(Boolean autoTrustSerializeClass) {
        this.autoTrustSerializeClass = autoTrustSerializeClass;
    }

    public Integer getTrustSerializeClassLevel() {
        return trustSerializeClassLevel;
    }

    public void setTrustSerializeClassLevel(Integer trustSerializeClassLevel) {
        this.trustSerializeClassLevel = trustSerializeClassLevel;
    }

    public Boolean getCheckSerializable() {
        return checkSerializable;
    }

    public void setCheckSerializable(Boolean checkSerializable) {
        this.checkSerializable = checkSerializable;
    }

    public void setExecutorManagementMode(String executorManagementMode) {
        this.executorManagementMode = executorManagementMode;
    }

    @Parameter(key = EXECUTOR_MANAGEMENT_MODE)
    public String getExecutorManagementMode() {
        return executorManagementMode;
    }

    @Parameter(excluded = true)
    public Boolean getOnlyUseMetadataV2() {
        return onlyUseMetadataV2;
    }

    public void setOnlyUseMetadataV2(Boolean onlyUseMetadataV2) {
        this.onlyUseMetadataV2 = onlyUseMetadataV2;
    }

    @Override
    public void refresh() {
        super.refresh();
        appendEnvironmentProperties();
        if (StringUtils.isEmpty(getName())) {
            this.setName(DEFAULT_APP_NAME);
            LOGGER.warn(
                    COMMON_UNEXPECTED_EXCEPTION,
                    "",
                    "",
                    "No application name was set, '" + DEFAULT_APP_NAME
                            + "' will be used as the default application name,"
                            + " it's highly recommended to set a unique and customized name for it can be critical for some service governance features.");
        }
    }

    private void appendEnvironmentProperties() {
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        Set<InfraAdapter> adapters = this.getExtensionLoader(InfraAdapter.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(adapters)) {
            Map<String, String> inputParameters = new HashMap<>();
            inputParameters.put(APPLICATION_KEY, getName());
            inputParameters.put(HOST_KEY, getHostname());
            for (InfraAdapter adapter : adapters) {
                Map<String, String> extraParameters = adapter.getExtraAttributes(inputParameters);
                if (CollectionUtils.isNotEmptyMap(extraParameters)) {
                    extraParameters.forEach((key, value) -> {
                        for (String prefix : this.getPrefixes()) {
                            prefix += ".";
                            if (key.startsWith(prefix)) {
                                key = key.substring(prefix.length());
                            }
                            parameters.put(key, value);
                            break;
                        }
                    });
                }
            }
        }
    }
}
