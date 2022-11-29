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
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT_COMPATIBLE;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_MODE_KEY;
import static org.apache.dubbo.config.Constants.DEVELOPMENT_ENVIRONMENT;
import static org.apache.dubbo.config.Constants.PRODUCTION_ENVIRONMENT;
import static org.apache.dubbo.config.Constants.TEST_ENVIRONMENT;


/**
 * The application info
 *
 * @export
 */
public class ApplicationConfig extends AbstractConfig {
    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ApplicationConfig.class);

    private static final long serialVersionUID = 5508512956753757169L;

    /**
     * Application name
     */
    private String name;

    /**
     * The application version
     */
    private String version;

    /**
     * Application owner
     */
    private String owner;

    /**
     * Application's organization (BU)
     */
    private String organization;

    /**
     * Architecture layer
     */
    private String architecture;

    /**
     * Environment, e.g. dev, test or production
     */
    private String environment;

    /**
     * Java compiler
     */
    private String compiler;

    /**
     * The type of the log access
     */
    private String logger;

    /**
     * Registry centers
     */
    private List<RegistryConfig> registries;
    private String registryIds;

    /**
     * Monitor center
     */
    private MonitorConfig monitor;

    /**
     * Directory for saving thread dump
     */
    private String dumpDirectory;

    /**
     * Whether to enable qos or not
     */
    private Boolean qosEnable;

    /**
     * The qos host to listen
     */
    private String qosHost;

    /**
     * The qos port to listen
     */
    private Integer qosPort;

    /**
     * Should we accept foreign ip or not?
     */
    private Boolean qosAcceptForeignIp;

    /**
     * When we disable accept foreign ip, support specify foreign ip in the whitelist
     */
    private String qosAcceptForeignIpWhitelist;

    /**
     * Customized parameters
     */
    private Map<String, String> parameters;

    /**
     * Config the shutdown.wait
     */
    private String shutwait;

    private String hostname;

    /**
     * Metadata type, local or remote, if choose remote, you need to further specify metadata center.
     */
    private String metadataType;

    /**
     * Used to control whether register instance to registry or not. Set to 'false' only when instance is pure consumer.
     */
    private Boolean registerConsumer;

    private String repository;

    private Boolean enableFileCache;

    /**
     * The preferred protocol(name) of this application
     * convenient for places where it's hard to determine which is the preferred protocol
     */
    private String protocol;

    /**
     * The protocol used for peer-to-peer metadata transmission
     */
    private String metadataServiceProtocol;

    /**
     * Metadata Service, used in Service Discovery
     */
    private Integer metadataServicePort;

    /**
     * used to set extensions of probe in qos
     */
    private String livenessProbe;

    private String readinessProbe;

    private String startupProbe;

    private String registerMode;

    private Boolean enableEmptyProtection;

    public ApplicationConfig() {
    }

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
                LOGGER.warn(COMMON_UNEXPECTED_EXCEPTION,"","","Failed to get the hostname of current instance.", e);
                hostname = "UNKNOWN";
            }
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
        if (environment != null && !(DEVELOPMENT_ENVIRONMENT.equals(environment)
            || TEST_ENVIRONMENT.equals(environment)
            || PRODUCTION_ENVIRONMENT.equals(environment))) {

            throw new IllegalStateException(String.format("Unsupported environment: %s, only support %s/%s/%s, default is %s.",
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
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>(1);
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
        return compiler;
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
        AdaptiveCompiler.setDefaultCompiler(compiler);
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

    @Parameter(key = QOS_ENABLE)
    public Boolean getQosEnable() {
        return qosEnable;
    }

    public void setQosEnable(Boolean qosEnable) {
        this.qosEnable = qosEnable;
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

    /**
     * The format is the same as the springboot, including: getQosEnableCompatible(), getQosPortCompatible(), getQosAcceptForeignIpCompatible().
     *
     * @return
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

    @Override
    public void refresh() {
        super.refresh();
        appendEnvironmentProperties();
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
