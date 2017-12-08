package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;

/**
 * {@link DubboConfigConfiguration} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
public class DubboConfigConfigurationTest {

    private AnnotationConfigApplicationContext context;

    @Before
    public void before() throws IOException {

        context = new AnnotationConfigApplicationContext();
        ResourcePropertySource propertySource = new ResourcePropertySource("META-INF/config.properties");
        context.getEnvironment().getPropertySources().addFirst(propertySource);

    }

    @After
    public void after() {
        context.close();
    }

    @Test
    public void testSingle() throws IOException {

        context.register(DubboConfigConfiguration.Single.class);
        context.refresh();

        // application
        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());

        // module
        ModuleConfig moduleConfig = context.getBean("moduleBean", ModuleConfig.class);
        Assert.assertEquals("dubbo-demo-module", moduleConfig.getName());

        // registry
        RegistryConfig registryConfig = context.getBean(RegistryConfig.class);
        Assert.assertEquals("zookeeper://192.168.99.100:32770", registryConfig.getAddress());

        // protocol
        ProtocolConfig protocolConfig = context.getBean(ProtocolConfig.class);
        Assert.assertEquals("dubbo", protocolConfig.getName());
        Assert.assertEquals(Integer.valueOf(20880), protocolConfig.getPort());
    }

    @Test
    public void testMultiple() {

        context.register(DubboConfigConfiguration.Multiple.class);
        context.refresh();

        // application
        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);
        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());

        ApplicationConfig applicationBean2 = context.getBean("applicationBean2", ApplicationConfig.class);
        Assert.assertEquals("dubbo-demo-application2", applicationBean2.getName());

        ApplicationConfig applicationBean3 = context.getBean("applicationBean3", ApplicationConfig.class);
        Assert.assertEquals("dubbo-demo-application3", applicationBean3.getName());

    }

}
