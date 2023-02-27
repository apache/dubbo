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
package org.apache.dubbo.config.spring.beans.factory.annotation;


import org.apache.dubbo.config.ArgumentConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ModuleConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.annotation.Argument;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.spring.api.HelloService;
import org.apache.dubbo.config.spring.impl.NotifyService;
import org.apache.dubbo.config.spring.reference.ReferenceCreator;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import com.alibaba.spring.util.AnnotationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.utils.CollectionUtils.ofSet;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.util.ReflectionUtils.findField;

/**
 * {@link ReferenceCreator} Test
 *
 * @see ReferenceCreator
 * @see DubboReference
 * @see Reference
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ReferenceCreatorTest.class, ReferenceCreatorTest.ConsumerConfiguration.class})
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class ReferenceCreatorTest {

    private static final String MODULE_CONFIG_ID = "mymodule";
    private static final String CONSUMER_CONFIG_ID = "myconsumer";
    private static final String MONITOR_CONFIG_ID = "mymonitor";
    private static final String REGISTRY_CONFIG_ID = "myregistry";

    @DubboReference(
        //interfaceClass = HelloService.class,
        version = "1.0.0", group = "TEST_GROUP", url = "dubbo://localhost:12345",
        client = "client", generic = false, injvm = false,
        check = false, init = false, lazy = true,
        stubevent = true, reconnect = "reconnect", sticky = true,
        proxy = "javassist", stub = "org.apache.dubbo.config.spring.api.HelloService", cluster = "failover",
        connections = 3, callbacks = 1, onconnect = "onconnect", ondisconnect = "ondisconnect",
        owner = "owner", layer = "layer", retries = 1,
        loadbalance = "random", async = true, actives = 3,
        sent = true, mock = "mock", validation = "validation",
        timeout = 3, cache = "cache", filter = {"echo", "generic", "accesslog"},
        listener = {"deprecated"}, parameters = {"n1=v1  ", "n2 = v2 ", "  n3 =   v3  "},
        application = "application",
        module = MODULE_CONFIG_ID, consumer = CONSUMER_CONFIG_ID, monitor = MONITOR_CONFIG_ID, registry = {REGISTRY_CONFIG_ID},
        // @since 2.7.3
        id = "reference",
        // @since 2.7.8
        services = {"service1", "service2", "service3", "service2", "service1"},
        providedBy = {"service1", "service2", "service3"},
        methods = @Method(name = "sayHello",
            isReturn = false,
            loadbalance = "loadbalance",
            oninvoke = "notifyService.onInvoke",
            onreturn = "notifyService.onReturn",
            onthrow = "notifyService.onThrow",
            timeout = 1000,
            retries = 2,
            parameters = {"a", "1", "b", "2"},
            arguments = @Argument(index = 0, callback = true)
        )
    )
    private HelloService helloService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private NotifyService notifyService;

    @BeforeAll
    public static void setUp() {
        DubboBootstrap.reset();
    }

    @Test
    void testBuild() throws Exception {

        Field helloServiceField = findField(getClass(), "helloService");
        DubboReference reference = findAnnotation(helloServiceField, DubboReference.class);
        // filter default value
        AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes(reference, true);
        ReferenceConfig referenceBean = ReferenceCreator.create(attributes, context)
            .defaultInterfaceClass(helloServiceField.getType())
            .build();
        Assertions.assertEquals(HelloService.class, referenceBean.getInterfaceClass());
        Assertions.assertEquals("org.apache.dubbo.config.spring.api.HelloService", referenceBean.getInterface());
        Assertions.assertEquals("1.0.0", referenceBean.getVersion());
        Assertions.assertEquals("TEST_GROUP", referenceBean.getGroup());
        Assertions.assertEquals("dubbo://localhost:12345", referenceBean.getUrl());
        Assertions.assertEquals("client", referenceBean.getClient());
        Assertions.assertEquals(null, referenceBean.isGeneric());
        Assertions.assertEquals(false, referenceBean.isInjvm());
        Assertions.assertEquals(false, referenceBean.isCheck());
        Assertions.assertEquals(false, referenceBean.isInit());
        Assertions.assertEquals(true, referenceBean.getLazy());
        Assertions.assertEquals(true, referenceBean.getStubevent());
        Assertions.assertEquals("reconnect", referenceBean.getReconnect());
        Assertions.assertEquals(true, referenceBean.getSticky());
        Assertions.assertEquals("javassist", referenceBean.getProxy());
        Assertions.assertEquals("org.apache.dubbo.config.spring.api.HelloService", referenceBean.getStub());
        Assertions.assertEquals("failover", referenceBean.getCluster());
        Assertions.assertEquals(Integer.valueOf(3), referenceBean.getConnections());
        Assertions.assertEquals(Integer.valueOf(1), referenceBean.getCallbacks());
        Assertions.assertEquals("onconnect", referenceBean.getOnconnect());
        Assertions.assertEquals("ondisconnect", referenceBean.getOndisconnect());
        Assertions.assertEquals("owner", referenceBean.getOwner());
        Assertions.assertEquals("layer", referenceBean.getLayer());
        Assertions.assertEquals(Integer.valueOf(1), referenceBean.getRetries());
        Assertions.assertEquals("random", referenceBean.getLoadbalance());
        Assertions.assertEquals(true, referenceBean.isAsync());
        Assertions.assertEquals(Integer.valueOf(3), referenceBean.getActives());
        Assertions.assertEquals(true, referenceBean.getSent());
        Assertions.assertEquals("mock", referenceBean.getMock());
        Assertions.assertEquals("validation", referenceBean.getValidation());
        Assertions.assertEquals(Integer.valueOf(3), referenceBean.getTimeout());
        Assertions.assertEquals("cache", referenceBean.getCache());
        Assertions.assertEquals("echo,generic,accesslog", referenceBean.getFilter());
        Assertions.assertEquals("deprecated", referenceBean.getListener());
        Assertions.assertEquals("reference", referenceBean.getId());
        Assertions.assertEquals(ofSet("service1", "service2", "service3"), referenceBean.getSubscribedServices());
        Assertions.assertEquals("service1,service2,service3", referenceBean.getProvidedBy());
        Assertions.assertEquals(REGISTRY_CONFIG_ID, referenceBean.getRegistryIds());

        // parameters
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("n1", "v1");
        parameters.put("n2", "v2");
        parameters.put("n3", "v3");
        Assertions.assertEquals(parameters, referenceBean.getParameters());

        // methods
        List<MethodConfig> methods = referenceBean.getMethods();
        Assertions.assertNotNull(methods);
        Assertions.assertEquals(1, methods.size());
        MethodConfig methodConfig = methods.get(0);
        Assertions.assertEquals("sayHello", methodConfig.getName());
        Assertions.assertEquals(false, methodConfig.isReturn());
        Assertions.assertEquals(1000, methodConfig.getTimeout());
        Assertions.assertEquals(2, methodConfig.getRetries());
        Assertions.assertEquals("loadbalance", methodConfig.getLoadbalance());
        Assertions.assertEquals(notifyService, methodConfig.getOninvoke());
        Assertions.assertEquals(notifyService, methodConfig.getOnreturn());
        Assertions.assertEquals(notifyService, methodConfig.getOnthrow());
        Assertions.assertEquals("onInvoke", methodConfig.getOninvokeMethod());
        Assertions.assertEquals("onReturn", methodConfig.getOnreturnMethod());
        Assertions.assertEquals("onThrow", methodConfig.getOnthrowMethod());
        // method parameters
        Map<String, String> methodParameters = new HashMap<String, String>();
        methodParameters.put("a", "1");
        methodParameters.put("b", "2");
        Assertions.assertEquals(methodParameters, methodConfig.getParameters());

        // method arguments
        List<ArgumentConfig> arguments = methodConfig.getArguments();
        Assertions.assertEquals(1, arguments.size());
        ArgumentConfig argumentConfig = arguments.get(0);
        Assertions.assertEquals(0, argumentConfig.getIndex());
        Assertions.assertEquals(true, argumentConfig.isCallback());

        // Asserts Null fields
        Assertions.assertThrows(IllegalStateException.class, referenceBean::getApplication);
        Assertions.assertNotNull(referenceBean.getModule());
        Assertions.assertNotNull(referenceBean.getConsumer());
        Assertions.assertNotNull(referenceBean.getMonitor());
    }


    @Configuration
    public static class ConsumerConfiguration {

        @Bean
        public NotifyService notifyService() {
            return new NotifyService();
        }

        @Bean("org.apache.dubbo.rpc.model.ModuleModel")
        public ModuleModel moduleModel() {
            return ApplicationModel.defaultModel().getDefaultModule();
        }

        @Bean(CONSUMER_CONFIG_ID)
        public ConsumerConfig consumerConfig() {
            return new ConsumerConfig();
        }

        @Bean(MONITOR_CONFIG_ID)
        public MonitorConfig monitorConfig() {
            return new MonitorConfig();
        }

        @Bean(MODULE_CONFIG_ID)
        public ModuleConfig moduleConfig() {
            return new ModuleConfig();
        }

    }

}