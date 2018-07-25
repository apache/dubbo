/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.url;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.mock.MockRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;


public class InvokerSideConfigUrlTest extends UrlTestBase {
    private static final Logger log = LoggerFactory.getLogger(InvokerSideConfigUrlTest.class);

    // ======================================================
    //   invoker related data preparing
    // ======================================================  
    private ApplicationConfig appConfForConsumer;
    private ApplicationConfig appConfForReference;
    private RegistryConfig regConfForConsumer;
    private RegistryConfig regConfForReference;
    private MethodConfig methodConfForReference;
    private ConsumerConfig consumerConf;
    private ReferenceConfig<DemoService> refConf;

    private Object appConfForConsumerTable[][] = {
            {"", "", "", "", "", "", "", "", "", ""},
    };

    private Object appConfForReferenceTable[][] = {
            {"", "", "", "", "", "", "", "", "", ""},
    };

    private Object regConfForConsumerTable[][] = {
//            {"timeout", "registry.timeout", "int", 5000, 9000, "", "", "", "", ""}, 
//            {"file", "registry.file", "string", "", "regConfForServiceTable.log", "", "", "", "", ""}, 
//            {"wait", "registry.wait", "int", 0, 9000, "", "", "", "", ""}, 
//            {"transport", "registry.transporter", "string", "netty", "mina", "", "", "", "", ""}, 
            {"subscribe", "subscribe", "boolean", true, false, "", "", "", "", ""},
            {"dynamic", "dynamic", "boolean", true, false, "", "", "", "", ""},
    };

    private Object regConfForReferenceTable[][] = {
            {"timeout", "registry.timeout", "int", 5000, 9000, "", "", "", "", ""},
            {"file", "registry.file", "string", "", "regConfForServiceTable.log", "", "", "", "", ""},
            {"wait", "registry.wait", "int", 0, 9000, "", "", "", "", ""},
            {"transport", "registry.transporter", "string", "netty", "mina", "", "", "", "", ""},
            {"subscribe", "subscribe", "boolean", true, false, "", "", "", "", ""},
            {"dynamic", "dynamic", "boolean", true, false, "", "", "", "", ""},
    };

    private Object methodConfForReferenceTable[][] = {
            {"actives", "eatTiger.actives", "int", 0, 90, "", "", "", "", ""},
            {"executes", "eatTiger.executes", "int", 0, 90, "", "", "", "", ""},
            {"deprecated", "eatTiger.deprecated", "boolean", false, true, "", "", "", "", ""},
            {"async", "eatTiger.async", "boolean", false, true, "", "", "", "", ""},
            {"timeout", "eatTiger.timeout", "int", 0, 90, "", "", "", "", ""},
    };

    private Object refConfTable[][] = {
//            {"version", "version", "string", "0.0.0", "1.2.3", "", "", "", "", ""}, 
//            {"group", "group", "string", "", "HaominTest", "", "", "", "", ""}, 

//            {"delay", "delay", "int", 0, 5, "", "", "", "", ""}, // not boolean 
            {"timeout", "timeout", "int", 5000, 3000, "", "", "", "", ""},
            {"retries", "retries", "int", 2, 5, "", "", "", "", ""},
            {"connections", "connections", "boolean", 100, 20, "", "", "", "", ""},
            {"loadbalance", "loadbalance", "string", "random", "roundrobin", "leastactive", "", "", ""},
            {"async", "async", "boolean", false, true, "", "", "", "", ""},
            //excluded = true
//            {"generic", "generic", "boolean", false, true, "", "", "", "", ""},  
            {"check", "check", "boolean", false, true, "", "", "", "", ""},
            //{"local", "local", "string", "false", "HelloServiceLocal", "true", "", "", "", ""}, 
            //{"local", "local", "string", "false", "true", "", "", "", "", ""}, 
            //{"mock", "mock", "string", "false", "dubbo.test.HelloServiceMock", "true", "", "", "", ""}, 
            {"mock", "mock", "string", "false", "false", "", "", "", "", ""},
            {"proxy", "proxy", "boolean", "javassist", "jdk", "", "", "", "", ""},
            {"client", "client", "string", "netty", "mina", "", "", "", "", ""},
            {"client", "client", "string", "netty", "mina", "", "", "", "", ""},
            {"owner", "owner", "string", "", "haomin,ludvik", "", "", "", "", ""},
            {"actives", "actives", "int", 0, 30, "", "", "", "", ""},
            {"cluster", "cluster", "string", "failover", "failfast", "failsafe", "failback", "forking", "", ""},
            //excluded = true
//            {"filter", "service.filter", "string", "default", "-generic", "", "", "", "", ""}, 
            //excluded = true
//            {"listener", "exporter.listener", "string", "default", "-deprecated", "", "", "", "", ""}, 
            //{"", "", "", "", "", "", "", "", "", ""}, 
    };

    private Object consumerConfTable[][] = {
            {"timeout", "default.timeout", "int", 5000, 8000, "", "", "", "", ""},
            {"retries", "default.retries", "int", 2, 5, "", "", "", "", ""},
            {"loadbalance", "default.loadbalance", "string", "random", "leastactive", "", "", "", "", ""},
            {"async", "default.async", "boolean", false, true, "", "", "", "", ""},
            {"connections", "default.connections", "int", 100, 5, "", "", "", "", ""},
//            {"generic", "generic", "boolean", false, false, "", "", "", "", ""}, 
            {"check", "check", "boolean", true, false, "", "", "", "", ""},
            {"proxy", "proxy", "string", "javassist", "jdk", "javassist", "", "", "", ""},
            {"owner", "owner", "string", "", "haomin", "", "", "", "", ""},
            {"actives", "default.actives", "int", 0, 5, "", "", "", "", ""},
            {"cluster", "default.cluster", "string", "failover", "forking", "", "", "", "", ""},
            {"filter", "", "string", "", "", "", "", "", "", ""},
            {"listener", "", "string", "", "", "", "", "", "", ""},
//            {"", "", "", "", "", "", "", "", "", ""}, 
    };

    // ======================================================
    //   test Start
    // ====================================================== 

    @BeforeClass
    public static void start() {
        //RegistryController.startRegistryIfAbsence(1);
    }


    @Before
    public void setUp() {
        initServConf();
        initRefConf();
    }

    @After()
    public void teardown() {
        //RegistryServer.reloadCache();
    }


    @Test
    public void consumerConfUrlTest() {
        verifyInvokerUrlGeneration(consumerConf, consumerConfTable);
    }

    @Test
    public void refConfUrlTest() {
        verifyInvokerUrlGeneration(refConf, refConfTable);
    }

    @Ignore("parameter on register center will not be merged any longer with query parameter request from the consumer")
    @Test
    public void regConfForConsumerUrlTest() {
        verifyInvokerUrlGeneration(regConfForConsumer, regConfForConsumerTable);
    }

    // ======================================================
    //   private helper
    // ====================================================== 
    private void initRefConf() {

        appConfForConsumer = new ApplicationConfig();
        appConfForReference = new ApplicationConfig();
        regConfForConsumer = new RegistryConfig();
        regConfForReference = new RegistryConfig();
        methodConfForReference = new MethodConfig();

        refConf = new ReferenceConfig<DemoService>();
        consumerConf = new ConsumerConfig();

        methodConfForReference.setName("sayName");
        regConfForReference.setAddress("127.0.0.1:9090");
        regConfForReference.setProtocol("mockregistry");
        appConfForReference.setName("ConfigTests");
        refConf.setInterface("org.apache.dubbo.config.api.DemoService");

        refConf.setApplication(appConfForReference);
        consumerConf.setApplication(appConfForConsumer);

        refConf.setRegistry(regConfForReference);
        consumerConf.setRegistry(regConfForConsumer);

        refConf.setConsumer(consumerConf);

        refConf.setMethods(Arrays.asList(new MethodConfig[]{methodConfForReference}));

        refConf.setScope(Constants.SCOPE_REMOTE);
    }

    private <T> void verifyInvokerUrlGeneration(T config, Object[][] dataTable) {
        servConf.export();

        fillConfigs(config, dataTable, TESTVALUE1);
        refConf.get();

        String subScribedUrlStr = getSubscribedUrlString();

        System.out.println("url string=========:" + subScribedUrlStr);
        String configName = config.getClass().getName();
        int column = TESTVALUE1;

        assertUrlStringWithLocalTable(subScribedUrlStr, dataTable, configName, column);

        try {
            refConf.destroy();
        } catch (Exception e) {
        }
    }

    private String getSubscribedUrlString() {
        return MockRegistry.getSubscribedUrl().toString();
    }
}