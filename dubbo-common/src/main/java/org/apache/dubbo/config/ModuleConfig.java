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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The module info
 *
 * @export
 */
public class ModuleConfig extends AbstractConfig {

    private static final long serialVersionUID = 5508512956753757169L;

    /**
     * Module name
     */
    private String name;

    /**
     * Module version
     */
    private String version;

    /**
     * Module owner
     */
    private String owner;

    /**
     * Module's organization
     */
    private String organization;

    /**
     * Registry centers
     */
    private List<RegistryConfig> registries;

    /**
     * Monitor center
     */
    private MonitorConfig monitor;

    /**
     * Whether start module in background.
     * If start in background, do not await finish on Spring ContextRefreshedEvent.
     *
     * @see org.apache.dubbo.config.spring.context.DubboDeployApplicationListener
     */
    private Boolean background;

    /**
     * Weather the reference is referred asynchronously
     */
    private Boolean referAsync;

    /**
     * Thread num for asynchronous refer pool size
     */
    private Integer referThreadNum;

    /**
     * Weather the service is export asynchronously
     */
    private Boolean exportAsync;

    /**
     * Thread num for asynchronous export pool size
     */
    private Integer exportThreadNum;

    public ModuleConfig() {
        super();
    }

    public ModuleConfig(ModuleModel moduleModel) {
        super(moduleModel);
    }

    public ModuleConfig(String name) {
        this();
        setName(name);
    }

    public ModuleConfig(ModuleModel moduleModel, String name) {
        this(moduleModel);
        setName(name);
    }

    @Override
    protected void checkDefault() {
        super.checkDefault();
        // default is false
        if (background == null) {
            background = false;
        }
    }

    @Override
    protected void checkScopeModel(ScopeModel scopeModel) {
        if (scopeModel == null) {
            throw new IllegalArgumentException("scopeModel cannot be null");
        }
        if (!(scopeModel instanceof ModuleModel)) {
            throw new IllegalArgumentException("Invalid scope model, expect to be a ModuleModel but got: " + scopeModel);
        }
    }

    @Override
    public ModuleModel getScopeModel() {
        return (ModuleModel) super.getScopeModel();
    }

    @Override
    protected ScopeModel getDefaultModel() {
        return ApplicationModel.defaultModel().getDefaultModule();
    }

    @Parameter(key = "module")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Parameter(key = "module.version")
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Parameter(key = "module.owner")
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Parameter(key = "module.organization")
    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
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

    public MonitorConfig getMonitor() {
        return monitor;
    }

    public void setMonitor(MonitorConfig monitor) {
        this.monitor = monitor;
    }

    public void setMonitor(String monitor) {
        this.monitor = new MonitorConfig(monitor);
    }

    public Boolean getBackground() {
        return background;
    }

    /**
     * Whether start module in background.
     * If start in backgound, do not await finish on Spring ContextRefreshedEvent.
     *
     * @see org.apache.dubbo.config.spring.context.DubboDeployApplicationListener
     */
    public void setBackground(Boolean background) {
        this.background = background;
    }

    public Integer getReferThreadNum() {
        return referThreadNum;
    }

    public void setReferThreadNum(Integer referThreadNum) {
        this.referThreadNum = referThreadNum;
    }

    public Integer getExportThreadNum() {
        return exportThreadNum;
    }

    public void setExportThreadNum(Integer exportThreadNum) {
        this.exportThreadNum = exportThreadNum;
    }

    public Boolean getReferAsync() {
        return referAsync;
    }

    public void setReferAsync(Boolean referAsync) {
        this.referAsync = referAsync;
    }

    public Boolean getExportAsync() {
        return exportAsync;
    }

    public void setExportAsync(Boolean exportAsync) {
        this.exportAsync = exportAsync;
    }
}
