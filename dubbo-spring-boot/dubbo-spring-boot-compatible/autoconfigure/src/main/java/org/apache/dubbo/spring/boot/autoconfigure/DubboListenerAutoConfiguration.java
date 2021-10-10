package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.spring.boot.context.event.AwaitingNonWebApplicationListener;
import org.apache.dubbo.spring.boot.context.event.DubboConfigBeanDefinitionConflictApplicationListener;
import org.apache.dubbo.spring.boot.context.event.WelcomeLogoApplicationListener;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;

/**
 * Dubbo Listener Auto-{@link Configuration}
 *
 * @since 3.0.4
 */
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true)
@Configuration
public class DubboListenerAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public DubboConfigBeanDefinitionConflictApplicationListener dubboConfigBeanDefinitionConflictApplicationListener() {
        return new DubboConfigBeanDefinitionConflictApplicationListener();
    }

    @ConditionalOnMissingBean
    @Bean
    public WelcomeLogoApplicationListener welcomeLogoApplicationListener() {
        return new WelcomeLogoApplicationListener();
    }

    @ConditionalOnMissingBean
    @Bean
    public AwaitingNonWebApplicationListener awaitingNonWebApplicationListener() {
        return new AwaitingNonWebApplicationListener();
    }
}
