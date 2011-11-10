/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.common.Constants;


/**
 * ApplicationConfig
 * 
 * @author william.liangf
 */
public class ApplicationConfig extends AbstractConfig {

	private static final long serialVersionUID = 5508512956753757169L;
	
    // 应用名称
    private String            name;
    
    // 应用负责人
    private String            owner;

    // 组织名(BU或部门)
    private String            organization;
    
    // 分层
    private String            architecture;
    
    // 环境，如：dev/test/run
    private String            environment;

    // 注册中心
    protected List<RegistryConfig> registries;
    
    // 服务监控
    private MonitorConfig     monitor;
    
    public ApplicationConfig() {
    }
    
    public ApplicationConfig(String name) {
        setName(name);
    }
    
    @Parameter(key = Constants.APPLICATION_KEY, required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        checkName("owner", owner);
        this.owner = owner;
    }

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
	    checkName("organization", organization);
		this.organization = organization;
	}

	public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        checkName("architecture", architecture);
        this.architecture = architecture;
    }

    public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
	    checkName("environment", environment);
	    if(environment != null) {
            if (! ("develop".equals(environment) || "test".equals(environment) || "product".equals(environment))) {
                throw new IllegalStateException("Unsupported environment: " + environment + ", only support develop/test/product, default is product.");
            }
        }
		this.environment = environment;
	}

    public RegistryConfig getRegistry() {
        return registries == null || registries.size() == 0 ? null : registries.get(0);
    }

    public void setRegistry(RegistryConfig registry) {
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>(1);
        registries.add(registry);
        this.registries = registries;
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    @SuppressWarnings({ "unchecked" })
    public void setRegistries(List<? extends RegistryConfig> registries) {
        this.registries = (List<RegistryConfig>)registries;
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

}