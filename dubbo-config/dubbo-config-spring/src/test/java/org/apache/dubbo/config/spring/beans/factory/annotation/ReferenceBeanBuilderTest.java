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


import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.utils.CollectionUtils.ofSet;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ReflectionUtils.findField;

/**
 * {@link ReferenceBeanBuilder} Test
 *
 * @see ReferenceBeanBuilder
 * @see DubboReference
 * @see Reference
 * @since 2.6.4
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ReferenceBeanBuilderTest.class)
public class ReferenceBeanBuilderTest {

    @DubboReference(
            interfaceClass = CharSequence.class,
            interfaceName = "java.lang.CharSequence",
            version = "1.0.0", group = "TEST_GROUP", url = "dubbo://localhost:12345",
            client = "client", generic = true, injvm = true,
            check = false, init = false, lazy = true,
            stubevent = true, reconnect = "reconnect", sticky = true,
            proxy = "javassist", stub = "java.lang.CharSequence", cluster = "failover",
            connections = 3, callbacks = 1, onconnect = "onconnect", ondisconnect = "ondisconnect",
            owner = "owner", layer = "layer", retries = 1,
            loadbalance = "random", async = true, actives = 3,
            sent = true, mock = "mock", validation = "validation",
            timeout = 3, cache = "cache", filter = {"echo", "generic", "accesslog"},
            listener = {"deprecated"}, parameters = {"n1=v1  ", "n2 = v2 ", "  n3 =   v3  "},
            application = "application",
            module = "module", consumer = "consumer", monitor = "monitor", registry = {"registry"},
            // @since 2.7.3
            id = "reference",
            // @since 2.7.8
            services = {"service1", "service2", "service3", "service2", "service1"}
    )
    private static final Object TEST_FIELD = new Object();

    @Autowired
    private ApplicationContext context;

    @Before
    public void init() {
        ApplicationModel.reset();
    }

    @Test
    public void testBuild() throws Exception {
        DubboReference reference = findAnnotation(findField(getClass(), "TEST_FIELD"), DubboReference.class);
        AnnotationAttributes attributes = AnnotationUtils.getAnnotationAttributes(reference, false, false);
        ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder.create(attributes, context);
        beanBuilder.interfaceClass(CharSequence.class);
        ReferenceBean referenceBean = beanBuilder.build();
        Assert.assertEquals(CharSequence.class, referenceBean.getInterfaceClass());
        Assert.assertEquals("1.0.0", referenceBean.getVersion());
        Assert.assertEquals("TEST_GROUP", referenceBean.getGroup());
        Assert.assertEquals("dubbo://localhost:12345", referenceBean.getUrl());
        Assert.assertEquals("client", referenceBean.getClient());
        Assert.assertEquals(true, referenceBean.isGeneric());
        Assert.assertTrue(referenceBean.isInjvm());
        Assert.assertEquals(false, referenceBean.isCheck());
        Assert.assertFalse(referenceBean.isInit());
        Assert.assertEquals(true, referenceBean.getLazy());
        Assert.assertEquals(true, referenceBean.getStubevent());
        Assert.assertEquals("reconnect", referenceBean.getReconnect());
        Assert.assertEquals(true, referenceBean.getSticky());
        Assert.assertEquals("javassist", referenceBean.getProxy());
        Assert.assertEquals("java.lang.CharSequence", referenceBean.getStub());
        Assert.assertEquals("failover", referenceBean.getCluster());
        Assert.assertEquals(Integer.valueOf(3), referenceBean.getConnections());
        Assert.assertEquals(Integer.valueOf(1), referenceBean.getCallbacks());
        Assert.assertEquals("onconnect", referenceBean.getOnconnect());
        Assert.assertEquals("ondisconnect", referenceBean.getOndisconnect());
        Assert.assertEquals("owner", referenceBean.getOwner());
        Assert.assertEquals("layer", referenceBean.getLayer());
        Assert.assertEquals(Integer.valueOf(1), referenceBean.getRetries());
        Assert.assertEquals("random", referenceBean.getLoadbalance());
        Assert.assertEquals(true, referenceBean.isAsync());
        Assert.assertEquals(Integer.valueOf(3), referenceBean.getActives());
        Assert.assertEquals(true, referenceBean.getSent());
        Assert.assertEquals("mock", referenceBean.getMock());
        Assert.assertEquals("validation", referenceBean.getValidation());
        Assert.assertEquals(Integer.valueOf(3), referenceBean.getTimeout());
        Assert.assertEquals("cache", referenceBean.getCache());
        Assert.assertEquals("echo,generic,accesslog", referenceBean.getFilter());
        Assert.assertEquals("deprecated", referenceBean.getListener());
        Assert.assertEquals("reference", referenceBean.getId());
        Assert.assertEquals(ofSet("service1", "service2", "service3"), referenceBean.getSubscribedServices());

        // parameters
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("n1", "v1");
        parameters.put("n2", "v2");
        parameters.put("n3", "v3");
        Assert.assertEquals(parameters, referenceBean.getParameters());

        // Asserts Null fields
        Assertions.assertThrows(IllegalStateException.class, () -> referenceBean.getApplication());
        Assert.assertNull(referenceBean.getModule());
        Assert.assertNull(referenceBean.getConsumer());
        Assert.assertNull(referenceBean.getMonitor());
        Assert.assertEquals(Collections.emptyList(), referenceBean.getRegistries());
    }
}
