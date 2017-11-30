package com.alibaba.dubbo.test.provider;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Provider {@Link Configuration}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
@Configuration
@ImportResource("META-INF/spring/dubbo-provider.xml")
@DubboComponentScan
public class ProviderConfiguration {
}
