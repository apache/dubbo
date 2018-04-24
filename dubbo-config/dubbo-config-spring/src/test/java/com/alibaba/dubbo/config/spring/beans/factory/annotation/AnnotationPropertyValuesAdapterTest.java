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
package com.alibaba.dubbo.config.spring.beans.factory.annotation;


import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.config.spring.api.DemoService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.env.MockEnvironment;
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

        Assert.assertEquals(DemoService.class, referenceBean.getInterfaceClass());
        Assert.assertEquals("com.alibaba.dubbo.config.spring.api.DemoService", referenceBean.getInterface());
        Assert.assertEquals("1.0.0", referenceBean.getVersion());
        Assert.assertEquals("group", referenceBean.getGroup());
        Assert.assertEquals("dubbo://localhost:12345", referenceBean.getUrl());
        Assert.assertEquals("client", referenceBean.getClient());
        Assert.assertEquals(true, referenceBean.isGeneric());
        Assert.assertEquals(true, referenceBean.isInjvm());
        Assert.assertEquals(false, referenceBean.isCheck());
        Assert.assertEquals(true, referenceBean.isInit());
        Assert.assertEquals(true, referenceBean.getLazy());
        Assert.assertEquals(true, referenceBean.getStubevent());
        Assert.assertEquals("reconnect", referenceBean.getReconnect());
        Assert.assertEquals(true, referenceBean.getSticky());

        Assert.assertEquals("javassist", referenceBean.getProxy());

        Assert.assertEquals("stub", referenceBean.getStub());
        Assert.assertEquals("failover", referenceBean.getCluster());
        Assert.assertEquals(Integer.valueOf(1), referenceBean.getConnections());
        Assert.assertEquals(Integer.valueOf(1), referenceBean.getCallbacks());
        Assert.assertEquals("onconnect", referenceBean.getOnconnect());
        Assert.assertEquals("ondisconnect", referenceBean.getOndisconnect());
        Assert.assertEquals("owner", referenceBean.getOwner());
        Assert.assertEquals("layer", referenceBean.getLayer());
        Assert.assertEquals(Integer.valueOf(2), referenceBean.getRetries());
        Assert.assertEquals("random", referenceBean.getLoadbalance());
        Assert.assertEquals(true, referenceBean.isAsync());
        Assert.assertEquals(Integer.valueOf(1), referenceBean.getActives());
        Assert.assertEquals(true, referenceBean.getSent());
        Assert.assertEquals("mock", referenceBean.getMock());
        Assert.assertEquals("validation", referenceBean.getValidation());
        Assert.assertEquals(Integer.valueOf(2), referenceBean.getTimeout());
        Assert.assertEquals("cache", referenceBean.getCache());
        Assert.assertEquals("default,default", referenceBean.getFilter());
        Assert.assertEquals("default,default", referenceBean.getListener());

        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put("key1", "value1");

        Assert.assertEquals(data, referenceBean.getParameters());
        // Bean compare
        Assert.assertEquals(null, referenceBean.getApplication());
        Assert.assertEquals(null, referenceBean.getModule());
        Assert.assertEquals(null, referenceBean.getConsumer());
        Assert.assertEquals(null, referenceBean.getMonitor());
        Assert.assertEquals(null, referenceBean.getRegistry());

    }

    private static class TestBean {

        @Reference(
                interfaceClass = DemoService.class, interfaceName = "com.alibaba.dubbo.config.spring.api.DemoService", version = "${version}", group = "group",
                url = "${url}  ", client = "client", generic = true, injvm = true,
                check = false, init = true, lazy = true, stubevent = true,
                reconnect = "reconnect", sticky = true, proxy = "javassist", stub = "stub",
                cluster = "failover", connections = 1, callbacks = 1, onconnect = "onconnect",
                ondisconnect = "ondisconnect", owner = "owner", layer = "layer", retries = 2,
                loadbalance = "random", async = true, actives = 1, sent = true,
                mock = "mock", validation = "validation", timeout = 2, cache = "cache",
                filter = {"default", "default"}, listener = {"default", "default"}, parameters = {"key1", "value1"}, application = "application",
                module = "module", consumer = "consumer", monitor = "monitor", registry = {"registry1", "registry2"}
        )
        private DemoService demoService;

    }
}
