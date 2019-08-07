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
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_ENV_KEYS;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_LABELS;
import static org.apache.dubbo.common.constants.CommonConstants.DUMP_DIRECTORY;
import static org.apache.dubbo.common.constants.CommonConstants.EQUAL_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.SEMICOLON_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT_COMPATIBLE;
import static org.apache.dubbo.config.Constants.ARCHITECTURE;
import static org.apache.dubbo.config.Constants.DEVELOPMENT_ENVIRONMENT;
import static org.apache.dubbo.config.Constants.ENVIRONMENT;
import static org.apache.dubbo.config.Constants.NAME;
import static org.apache.dubbo.config.Constants.ORGANIZATION;
import static org.apache.dubbo.config.Constants.OWNER;
import static org.apache.dubbo.config.Constants.PRODUCTION_ENVIRONMENT;
import static org.apache.dubbo.config.Constants.TEST_ENVIRONMENT;


/**
 * The application info
 *
 * @export
 */
public class ApplicationConfig extends AbstractConfig {

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
     * Is default or not
     */
    private Boolean isDefault;

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
     * Customized parameters
     */
    private Map<String, String> parameters;

    /**
     * Config the shutdown.wait
     */
    private String shutwait;

    public ApplicationConfig() {
    }

    public ApplicationConfig(String name) {
        setName(name);
    }

    @Parameter(key = APPLICATION_KEY, required = true, useKeyAsProperty = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName(NAME, name);
        this.name = name;
        if (StringUtils.isEmpty(id)) {
            id = name;
        }
    }

    @Parameter(key = "application.version")
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
        checkMultiName(OWNER, owner);
        this.owner = owner;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        checkName(ORGANIZATION, organization);
        this.organization = organization;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        checkName(ARCHITECTURE, architecture);
        this.architecture = architecture;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        checkName(ENVIRONMENT, environment);
        if (environment != null) {
            if (!(DEVELOPMENT_ENVIRONMENT.equals(environment)
                    || TEST_ENVIRONMENT.equals(environment)
                    || PRODUCTION_ENVIRONMENT.equals(environment))) {

                throw new IllegalStateException(String.format("Unsupported environment: %s, only support %s/%s/%s, default is %s.",
                        environment,
                        DEVELOPMENT_ENVIRONMENT,
                        TEST_ENVIRONMENT,
                        PRODUCTION_ENVIRONMENT,
                        PRODUCTION_ENVIRONMENT));
            }
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
        LoggerFactory.setLoggerAdapter(logger);
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        this.isDefault = isDefault;
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

    /**
     * The format is the same as the springboot, including: getQosEnableCompatible(), getQosPortCompatible(), getQosAcceptForeignIpCompatible().
     * @return
     */
    @Parameter(key = QOS_ENABLE_COMPATIBLE, excluded = true)
    public Boolean getQosEnableCompatible() {
        return getQosEnable();
    }

    public void setQosEnableCompatible(Boolean qosEnable) {
        setQosEnable(qosEnable);
    }

    @Parameter(key = QOS_HOST_COMPATIBLE, excluded = true)
    public String getQosHostCompatible() {
        return getQosHost();
    }

    public void setQosHostCompatible(String qosHost) {
        this.setQosHost(qosHost);
    }

    @Parameter(key = QOS_PORT_COMPATIBLE, excluded = true)
    public Integer getQosPortCompatible() {
        return getQosPort();
    }

    public void setQosPortCompatible(Integer qosPort) {
        this.setQosPort(qosPort);
    }

    @Parameter(key = ACCEPT_FOREIGN_IP_COMPATIBLE, excluded = true)
    public Boolean getQosAcceptForeignIpCompatible() {
        return this.getQosAcceptForeignIp();
    }

    public void setQosAcceptForeignIpCompatible(Boolean qosAcceptForeignIp) {
        this.setQosAcceptForeignIp(qosAcceptForeignIp);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        checkParameterName(parameters);
        this.parameters = parameters;
    }

    public String getShutwait() {
        return shutwait;
    }

    public void setShutwait(String shutwait) {
        System.setProperty(SHUTDOWN_WAIT_KEY, shutwait);
        this.shutwait = shutwait;
    }

    @Override
    @Parameter(excluded = true)
    public boolean isValid() {
        return !StringUtils.isEmpty(name);
    }

    @Override
    public void refresh() {
        super.refresh();
        appendEnvironmentProperties();
    }

    /**
     * 1. OS Environment: DUBBO_LABELS=tag=pre;key=value
     * 2. JVM Options: -Denv_keys = DUBBO_KEY1, DUBBO_KEY2
     */
    private void appendEnvironmentProperties() {
        String rawLabels = ConfigurationUtils.getProperty(DUBBO_LABELS);
        if (StringUtils.isNotEmpty(rawLabels)) {
            String[] labelPairs = SEMICOLON_SPLIT_PATTERN.split(rawLabels);
            for (String pair : labelPairs) {
                String[] label = EQUAL_SPLIT_PATTERN.split(pair);
                if (label.length == 2) {
                    updateParameters(parameters, label[0], label[1]);
                }
            }
        }

        String rawKeys = ConfigurationUtils.getProperty(DUBBO_ENV_KEYS);
        if (StringUtils.isNotEmpty(rawKeys)) {
            String[] keys = COMMA_SPLIT_PATTERN.split(rawKeys);
            for (String key : keys) {
                String value = ConfigurationUtils.getProperty(key);
                if (value != null) {
                    updateParameters(parameters, key, value);
                }
            }
        }
    }

    private void updateParameters(Map<String, String> map, String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }

}
