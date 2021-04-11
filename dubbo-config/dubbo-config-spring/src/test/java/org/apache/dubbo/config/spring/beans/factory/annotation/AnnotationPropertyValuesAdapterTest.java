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


import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.api.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.DataBinder;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;

/**
 * {@link AnnotationPropertyValuesAdapter} Test
 *
 * @since 2.5.11
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AnnotationPropertyValuesAdapterTest {

    @Test
    public void test() {

        MockEnvironment mockEnvironment = new MockEnvironment();

        mockEnvironment.setProperty("version", "1.0.0");

        mockEnvironment.setProperty("url", "   dubbo://localhost:12345");

        Field field = ReflectionUtils.findField(TestBean.class, "demoService");

        Reference reference = AnnotationUtils.getAnnotation(field, Reference.class);

        AnnotationPropertyValuesAdapter propertyValues = new AnnotationPropertyValuesAdapter(reference, mockEnvironment);

        ReferenceBean referenceBean = new ReferenceBean();

        DataBinder dataBinder = new DataBinder(referenceBean);

        dataBinder.setDisallowedFields("application", "module", "consumer", "monitor", "registry");

        DefaultConversionService conversionService = new DefaultConversionService();

        conversionService.addConverter(new Converter<String[], String>() {
            @Override
            public String convert(String[] source) {
                return arrayToCommaDelimitedString(source);
            }
        });

        conversionService.addConverter(new Converter<String[], Map<String, String>>() {
            @Override
            public Map<String, String> convert(String[] source) {
                return CollectionUtils.toStringMap(source);
            }
        });


        dataBinder.setConversionService(conversionService);


        dataBinder.bind(propertyValues);

//        System.out.println(referenceBean);

        Assertions.assertEquals(DemoService.class, referenceBean.getInterfaceClass());
        Assertions.assertEquals("org.apache.dubbo.config.spring.api.DemoService", referenceBean.getInterface());
        Assertions.assertEquals("1.0.0", referenceBean.getVersion());
        Assertions.assertEquals("group", referenceBean.getGroup());
        Assertions.assertEquals("dubbo://localhost:12345", referenceBean.getUrl());
        Assertions.assertEquals("client", referenceBean.getClient());
        Assertions.assertEquals(true, referenceBean.isGeneric());
        Assertions.assertNull(referenceBean.isInjvm());
        Assertions.assertEquals(false, referenceBean.isCheck());
        Assertions.assertEquals(true, referenceBean.isInit());
        Assertions.assertEquals(true, referenceBean.getLazy());
        Assertions.assertEquals(true, referenceBean.getStubevent());
        Assertions.assertEquals("reconnect", referenceBean.getReconnect());
        Assertions.assertEquals(true, referenceBean.getSticky());

        Assertions.assertEquals("javassist", referenceBean.getProxy());

        Assertions.assertEquals("stub", referenceBean.getStub());
        Assertions.assertEquals("failover", referenceBean.getCluster());
        Assertions.assertEquals(Integer.valueOf(1), referenceBean.getConnections());
        Assertions.assertEquals(Integer.valueOf(1), referenceBean.getCallbacks());
        Assertions.assertEquals("onconnect", referenceBean.getOnconnect());
        Assertions.assertEquals("ondisconnect", referenceBean.getOndisconnect());
        Assertions.assertEquals("owner", referenceBean.getOwner());
        Assertions.assertEquals("layer", referenceBean.getLayer());
        Assertions.assertEquals(Integer.valueOf(1), referenceBean.getRetries());
        Assertions.assertEquals("random", referenceBean.getLoadbalance());
        Assertions.assertEquals(true, referenceBean.isAsync());
        Assertions.assertEquals(Integer.valueOf(1), referenceBean.getActives());
        Assertions.assertEquals(true, referenceBean.getSent());
        Assertions.assertEquals("mock", referenceBean.getMock());
        Assertions.assertEquals("validation", referenceBean.getValidation());
        Assertions.assertEquals(Integer.valueOf(2), referenceBean.getTimeout());
        Assertions.assertEquals("cache", referenceBean.getCache());
        Assertions.assertEquals("default,default", referenceBean.getFilter());
        Assertions.assertEquals("default,default", referenceBean.getListener());

        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("key1", "value1");

        Assertions.assertEquals(data, referenceBean.getParameters());
        // Bean compare
        Assertions.assertNull(referenceBean.getRegistry());

    }

    private static class TestBean {

        @Reference(
                interfaceClass = DemoService.class, interfaceName = "com.alibaba.dubbo.config.spring.api.DemoService", version = "${version}", group = "group",
                url = "${url}  ", client = "client", generic = true, injvm = true,
                check = false, init = true, lazy = true, stubevent = true,
                reconnect = "reconnect", sticky = true, proxy = "javassist", stub = "stub",
                cluster = "failover", connections = 1, callbacks = 1, onconnect = "onconnect",
                ondisconnect = "ondisconnect", owner = "owner", layer = "layer", retries = 1,
                loadbalance = "random", async = true, actives = 1, sent = true,
                mock = "mock", validation = "validation", timeout = 2, cache = "cache",
                filter = {"default", "default"}, listener = {"default", "default"}, parameters = {"key1", "value1"}, application = "application",
                module = "module", consumer = "consumer", monitor = "monitor", registry = {"registry1", "registry2"}
        )
        private DemoService demoService;

    }
}
