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
package org.apache.dubbo.spring.boot.actuate.autoconfigure;

import org.apache.dubbo.spring.boot.actuate.endpoint.DubboConfigsMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboDisableDetailProfilerEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboDisableRouterSnapshotEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboDisableSimpleProfilerEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboEnableDetailProfilerEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboEnableRouterSnapshotEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboEnableSimpleProfilerEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboGetEnabledRouterEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboGetRecentRouterEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboGracefulShutdownEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboLoggerInfoEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboLsEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboOfflineAppEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboOfflineEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboOfflineInterfaceEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboOnlineAppEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboOnlineEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboOnlineInterfaceEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboPropertiesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboReadyEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboReferencesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboServicesMetadataEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboSetProfilerWarnPercentEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboShutdownEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboSwitchLogLevelEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.DubboSwitchLoggerEndpoint;
import org.apache.dubbo.spring.boot.actuate.endpoint.condition.CompatibleConditionalOnEnabledEndpoint;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;

/**
 * Dubbo {@link Endpoint @Endpoint} Auto-{@link Configuration} for Spring Boot Actuator 2.0
 *
 * @see Endpoint
 * @see Configuration
 * @since 2.7.0
 */
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@Configuration
@PropertySource(
        name = "Dubbo Endpoints Default Properties",
        value = "classpath:/META-INF/dubbo-endpoints-default.properties")
public class DubboEndpointAnnotationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboMetadataEndpoint dubboEndpoint() {
        return new DubboMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboConfigsMetadataEndpoint dubboConfigsMetadataEndpoint() {
        return new DubboConfigsMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboPropertiesMetadataEndpoint dubboPropertiesEndpoint() {
        return new DubboPropertiesMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboReferencesMetadataEndpoint dubboReferencesMetadataEndpoint() {
        return new DubboReferencesMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboServicesMetadataEndpoint dubboServicesMetadataEndpoint() {
        return new DubboServicesMetadataEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboShutdownEndpoint dubboShutdownEndpoint() {
        return new DubboShutdownEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboOnlineEndpoint dubboOnlineEndpoint() {
        return new DubboOnlineEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboOnlineAppEndpoint dubboOnlineAppEndpoint() {
        return new DubboOnlineAppEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboOnlineInterfaceEndpoint dubboOnlineInterfaceEndpoint() {
        return new DubboOnlineInterfaceEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboOfflineEndpoint dubboOfflineEndpoint() {
        return new DubboOfflineEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboOfflineAppEndpoint dubboOfflineAppEndpoint() {
        return new DubboOfflineAppEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboOfflineInterfaceEndpoint dubboOfflineInterfaceEndpoint() {
        return new DubboOfflineInterfaceEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboReadyEndpoint dubboReadyEndpoint() {
        return new DubboReadyEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboLsEndpoint dubboLsEndpoint() {
        return new DubboLsEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboLoggerInfoEndpoint dubboLoggerInfoEndpoint() {
        return new DubboLoggerInfoEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboSwitchLoggerEndpoint dubboSwitchLoggerEndpoint() {
        return new DubboSwitchLoggerEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboSwitchLogLevelEndpoint dubboSwitchLogLevelEndpoint() {
        return new DubboSwitchLogLevelEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboDisableDetailProfilerEndpoint dubboDisableDetailProfilerEndpoint() {
        return new DubboDisableDetailProfilerEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboDisableSimpleProfilerEndpoint dubboDisableSimpleProfilerEndpoint() {
        return new DubboDisableSimpleProfilerEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboEnableDetailProfilerEndpoint dubboEnableDetailProfilerEndpoint() {
        return new DubboEnableDetailProfilerEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboEnableSimpleProfilerEndpoint dubboEnableSimpleProfilerEndpoint() {
        return new DubboEnableSimpleProfilerEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboSetProfilerWarnPercentEndpoint dubboSetProfilerWarnPercentEndpoint() {
        return new DubboSetProfilerWarnPercentEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboDisableRouterSnapshotEndpoint dubboDisableRouterSnapshotEndpoint() {
        return new DubboDisableRouterSnapshotEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboEnableRouterSnapshotEndpoint dubboEnableRouterSnapshotEndpoint() {
        return new DubboEnableRouterSnapshotEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboGetRecentRouterEndpoint dubboGetRecentRouterEndpoint() {
        return new DubboGetRecentRouterEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboGetEnabledRouterEndpoint dubboGetEnabledRouterEndpoint() {
        return new DubboGetEnabledRouterEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    @CompatibleConditionalOnEnabledEndpoint
    public DubboGracefulShutdownEndpoint dubboGracefulShutdownEndpoint() {
        return new DubboGracefulShutdownEndpoint();
    }
}
