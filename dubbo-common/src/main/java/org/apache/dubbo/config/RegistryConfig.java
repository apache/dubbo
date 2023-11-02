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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.ENABLE_EMPTY_PROTECTION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTER_MODE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_CLUSTER_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.PojoUtils.updatePropertyIfAbsent;

/**
 * Configuration for service registration and discovery.
 *
 * @export
 */
public class RegistryConfig extends AbstractConfig {

    private static final long serialVersionUID = 5508512956753757169L;

    public static final String NO_AVAILABLE = "N/A";

    /**
     * Register center address.
     */
    private String address;

    /**
     * Username to login the register center.
     */
    private String username;

    /**
     * Password to login the register center.
     */
    private String password;

    /**
     * Default port for the register center.
     */
    private Integer port;

    /**
     * Protocol used for the register center.
     */
    private String protocol;

    /**
     * Network transmission type.
     */
    private String transporter;

    /**
     * Server implementation.
     */
    private String server;

    /**
     * Client implementation.
     */
    private String client;

    /**
     * Affects how traffic distributes among registries, useful when subscribing to multiple registries.
     * Available options:
     * - "zone-aware": A certain type of traffic always goes to one Registry according to where the traffic is originated.
     */
    private String cluster;

    /**
     * The region where the registry belongs, usually used to isolate traffics.
     */
    private String zone;

    /**
     * The group that services registry belongs to.
     */
    private String group;

    /**
     * Version of the registry.
     */
    private String version;

    /**
     * Connect timeout in milliseconds for the register center.
     */
    private Integer timeout;

    /**
     * Session timeout in milliseconds for the register center.
     */
    private Integer session;

    /**
     * File for saving the register center dynamic list.
     */
    private String file;

    /**
     * Wait time before stopping.
     */
    private Integer wait;

    /**
     * Whether to check if the register center is available when booting up.
     */
    private Boolean check;

    /**
     * Whether to allow dynamic service registration on the register center.
     */
    private Boolean dynamic;

    /**
     * Whether to allow exporting service on the register center.
     */
    private Boolean register;

    /**
     * Whether to allow subscribing to services on the register center.
     */
    private Boolean subscribe;

    /**
     * Customized parameters.
     */
    private Map<String, String> parameters;

    /**
     * Simplify the registry, useful for both providers and consumers.
     *
     * @since 2.7.0
     */
    private Boolean simplified;

    /**
     * After simplifying the registry, add some parameters individually, useful for providers.
     * Example: extra-keys = "A, b, c, d".
     *
     * @since 2.7.0
     */
    private String extraKeys;

    /**
     * Indicates whether the address works as a configuration center or not.
     */
    private Boolean useAsConfigCenter;

    /**
     * Indicates whether the address works as a remote metadata center or not.
     */
    private Boolean useAsMetadataCenter;

    /**
     * List of RPC protocols accepted by this registry, e.g., "dubbo,rest".
     */
    private String accepts;

    /**
     * Always use this registry first if set to true, useful when subscribing to multiple registries.
     */
    private Boolean preferred;

    /**
     * Affects traffic distribution among registries, useful when subscribing to multiple registries.
     * Takes effect only when no preferred registry is specified.
     */
    private Integer weight;

    /**
     * Register mode.
     */
    private String registerMode;

    /**
     * Enable empty protection.
     */
    private Boolean enableEmptyProtection;

    /**
     * Security settings.
     */
    private String secure;

    public String getSecure() {
        return secure;
    }

    public void setSecure(String secure) {
        this.secure = secure;
    }

    public RegistryConfig() {
    }

    public RegistryConfig(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    public RegistryConfig(String address) {
        setAddress(address);
    }

    public RegistryConfig(ApplicationModel applicationModel, String address) {
        super(applicationModel);
        setAddress(address);
    }

    public RegistryConfig(String address, String protocol) {
        setAddress(address);
        setProtocol(protocol);
    }

    public RegistryConfig(ApplicationModel applicationModel, String address, String protocol) {
        super(applicationModel);
        setAddress(address);
        setProtocol(protocol);
    }

    @Override
    @Parameter(key = REGISTRY_CLUSTER_KEY)
    public String getId() {
        return super.getId();
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Parameter(excluded = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        if (address != null) {
            try {
                URL url = URL.valueOf(address);

                // Refactor since 2.7.8
                updatePropertyIfAbsent(this::getUsername, this::setUsername, url.getUsername());
                updatePropertyIfAbsent(this::getPassword, this::setPassword, url.getPassword());
                updatePropertyIfAbsent(this::getProtocol, this::setProtocol, url.getProtocol());
                updatePropertyIfAbsent(this::getPort, this::setPort, url.getPort());

                Map<String, String> params = url.getParameters();
                if (CollectionUtils.isNotEmptyMap(params)) {
                    params.remove(BACKUP_KEY);
                }
                updateParameters(params);
            } catch (Exception ignored) {
            }
        }
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return wait
     * @see org.apache.dubbo.config.ProviderConfig#getWait()
     * @deprecated
     */
    @Deprecated
    public Integer getWait() {
        return wait;
    }

    /**
     * @param wait
     * @see org.apache.dubbo.config.ProviderConfig#setWait(Integer)
     * @deprecated
     */
    @Deprecated
    public void setWait(Integer wait) {
        this.wait = wait;
        if (wait != null && wait > 0) {
            System.setProperty(SHUTDOWN_WAIT_KEY, String.valueOf(wait));
        }
    }

    public Boolean isCheck() {
        return check;
    }

    public void setCheck(Boolean check) {
        this.check = check;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    /**
     * @return transport
     * @see #getTransporter()
     * @deprecated
     */
    @Deprecated
    @Parameter(excluded = true, attribute = false)
    public String getTransport() {
        return getTransporter();
    }

    /**
     * @param transport
     * @see #setTransporter(String)
     * @deprecated
     */
    @Deprecated
    public void setTransport(String transport) {
        setTransporter(transport);
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        /*if(transporter != null && transporter.length() > 0 && ! this.getExtensionLoader(Transporter.class).hasExtension(transporter)){
            throw new IllegalStateException("No such transporter type : " + transporter);
        }*/
        this.transporter = transporter;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        /*if(server != null && server.length() > 0 && ! this.getExtensionLoader(Transporter.class).hasExtension(server)){
            throw new IllegalStateException("No such server type : " + server);
        }*/
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        /*if(client != null && client.length() > 0 && ! this.getExtensionLoader(Transporter.class).hasExtension(client)){
            throw new IllegalStateException("No such client type : " + client);
        }*/
        this.client = client;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Integer getSession() {
        return session;
    }

    public void setSession(Integer session) {
        this.session = session;
    }

    public Boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public Boolean isRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Boolean isSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void updateParameters(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return;
        }
        if (this.parameters == null) {
            this.parameters = parameters;
        } else {
            this.parameters.putAll(parameters);
        }
    }

    public Boolean getSimplified() {
        return simplified;
    }

    public void setSimplified(Boolean simplified) {
        this.simplified = simplified;
    }

    @Parameter(key = EXTRA_KEYS_KEY)
    public String getExtraKeys() {
        return extraKeys;
    }

    public void setExtraKeys(String extraKeys) {
        this.extraKeys = extraKeys;
    }

    @Parameter(excluded = true)
    public Boolean getUseAsConfigCenter() {
        return useAsConfigCenter;
    }

    public void setUseAsConfigCenter(Boolean useAsConfigCenter) {
        this.useAsConfigCenter = useAsConfigCenter;
    }

    @Parameter(excluded = true)
    public Boolean getUseAsMetadataCenter() {
        return useAsMetadataCenter;
    }

    public void setUseAsMetadataCenter(Boolean useAsMetadataCenter) {
        this.useAsMetadataCenter = useAsMetadataCenter;
    }

    public String getAccepts() {
        return accepts;
    }

    public void setAccepts(String accepts) {
        this.accepts = accepts;
    }

    public Boolean getPreferred() {
        return preferred;
    }

    public void setPreferred(Boolean preferred) {
        this.preferred = preferred;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
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

    @Override
    @Parameter(excluded = true, attribute = false)
    public boolean isValid() {
        // empty protocol will default to 'dubbo'
        return !StringUtils.isEmpty(address) || !StringUtils.isEmpty(protocol);
    }

    @Override
    @Parameter(excluded = true)
    public Boolean isDefault() {
        return isDefault;
    }
}
