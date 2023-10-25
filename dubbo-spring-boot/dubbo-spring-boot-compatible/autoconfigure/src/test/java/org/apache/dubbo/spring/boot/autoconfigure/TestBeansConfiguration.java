package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestBeansConfiguration {

    @Bean
    ApplicationConfig application1() {
        ApplicationConfig config = new ApplicationConfig();
        config.setId("application1");
        return config;
    }

    @Bean
    ModuleConfig module1() {
        ModuleConfig config = new ModuleConfig();
        config.setId("module1");
        return config;
    }

    @Bean
    RegistryConfig registry1() {
        RegistryConfig config = new RegistryConfig();
        config.setId("registry1");
        return config;
    }

    @Bean
    MonitorConfig monitor1() {
        MonitorConfig config = new MonitorConfig();
        config.setId("monitor1");
        return config;
    }

    @Bean
    ProtocolConfig protocol1() {
        ProtocolConfig config = new ProtocolConfig();
        config.setId("protocol1");
        return config;
    }

    @Bean
    ConsumerConfig consumer1() {
        ConsumerConfig config = new ConsumerConfig();
        config.setId("consumer1");
        return config;
    }

    @Bean
    ProviderConfig provider1() {
        ProviderConfig config = new ProviderConfig();
        config.setId("provider1");
        return config;
    }


}
