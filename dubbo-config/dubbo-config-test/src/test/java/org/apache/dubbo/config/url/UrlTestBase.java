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


import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.api.DemoService;
import org.apache.dubbo.config.provider.impl.DemoServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unused")
public class UrlTestBase {

    // ======================================================
    //   data column definition
    // ======================================================
    protected static final int KEY = 0;
    protected static final int URL_KEY = 1;
    protected static final int TESTVALUE1 = 4;
    private static final Logger log = LoggerFactory.getLogger(UrlTestBase.class);
    private static final int TYPE = 2;
    private static final int DEFAULT = 3;
    private static final int TESTVALUE2 = 5;
    private static final int TESTVALUE3 = 6;
    private static final int TESTVALUE4 = 7;
    private static final int TESTVALUE5 = 8;
    private static final int TESTVALUE6 = 9;
    private static final int TESTVALUE7 = 10;
    protected ApplicationConfig application;
    protected RegistryConfig regConfForProvider;
    protected RegistryConfig regConfForService;
    protected ProviderConfig provConf;
    protected ProtocolConfig protoConfForProvider;
    protected ProtocolConfig protoConfForService;
    protected MethodConfig methodConfForService;
    protected ServiceConfig<DemoService> servConf;
    protected Object servConfTable[][] = {
        {"proxy", "proxy", "string", "javassist", "jdk", "javassist", "", "", "", ""},
        {"actives", "actives", "int", 0, 90, "", "", "", "", ""},
        {"executes", "executes", "int", 0, 90, "", "", "", "", ""},
        {"deprecated", "deprecated", "boolean", false, true, "", "", "", "", ""},
        {"dynamic", "dynamic", "boolean", true, false, "", "", "", "", ""},
        {"accesslog", "accesslog", "string", "", "haominTest", "", "", "", "", ""},
        {"document", "document", "string", "", "http://dubbo.apache.org/zh-cn/docs/user/quick-start.html?testquery=你好你好", "", "", "", "", ""},
        {"weight", "weight", "int", 0, 90, "", "", "", "", ""},

        //{"filter", "service.filter", "string", "", "", "", "", "", "", ""},
        //{"listener", "listener", "string", "", "", "", "", "", "", ""},

    };
    protected Object regConfForServiceTable[][] = {
        //            {"timeout", "registry.timeout", "int", 5000, 9000, "", "", "", "", ""},
        //            {"file", "registry.file", "string", "", "regConfForServiceTable.log", "", "", "", "", ""},
        //            {"wait", "registry.wait", "int", 0, 9000, "", "", "", "", ""},
        //            {"transport", "registry.transporter", "string", "netty", "mina", "", "", "", "", ""},
        //            {"subscribe", "subscribe", "boolean", true, false, "", "", "", "", ""},
        {"dynamic", "dynamic", "boolean", true, false, "", "", "", "", ""},
    };
    protected Object provConfTable[][] = {{"cluster", "cluster", "string", "string", "failover", "failfast", "failsafe", "", "", ""}, {"async", "async", "boolean", false, true, "", "", "", "", ""}, {"loadbalance", "loadbalance", "string", "random", "leastactive", "", "", "", "", ""}, {"connections", "connections", "int", 0, 60, "", "", "", "", ""}, {"retries", "retries", "int", 2, 60, "", "", "", "", ""}, {"timeout", "timeout", "int", 5000, 60, "", "", "", "", ""},
        //change by fengting listener 没有缺省值
        //{"listener", "exporter.listener", "string", "", "", "", "", "", "", ""},
        //{"filter", "service.filter", "string", "", "", "", "", "", "", ""},

    };
    protected Object methodConfForServiceTable[][] = {
        {"actives", "sayName.actives", "int", 0, 90, "", "", "", "", ""},
        {"executes", "sayName.executes", "int", 0, 90, "", "", "", "", ""},
        {"deprecated", "sayName.deprecated", "boolean", false, true, "", "", "", "", ""},
        {"async", "sayName.async", "boolean", false, true, "", "", "", "", ""},
        {"timeout", "sayName.timeout", "int", 0, 90, "", "", "", "", ""},
    };
    protected DemoService demoService = new DemoServiceImpl();
    private Object appConfForProviderTable[][] = {
        {"", "", "", "", "", "", "", "", "", ""},
    };
    private Object appConfForServiceTable[][] = {
        {"", "", "", "", "", "", "", "", "", ""},
    };
    private Object regConfForProviderTable[][] = {
        {"", "", "", "", "", "", "", "", "", ""},
    };
    private Object protoConfForProviderTable[][] = {
        {"", "", "", "", "", "", "", "", "", ""},
    };
    private Object protoConfForServiceTable[][] = {
        {"", "", "", "", "", "", "", "", "", ""},
    };

    // ======================================================
    //   data table manipulation utils
    // ======================================================
    protected String genParamString(Object urlKey, Object value) {

        return (String) urlKey + "=" + value.toString();
    }

    protected <T> void fillConfigs(T conf, Object[][] table, int column) {

        for (Object[] row : table) {
            fillConfig(conf, row, column);
        }
    }

    protected <T> void fillConfig(T conf, Object[] row, int column) {

        RpcConfigGetSetProxy proxy = new RpcConfigGetSetProxy(conf);
        proxy.setValue((String) row[KEY], row[column]);

    }

    @SuppressWarnings("deprecation")
    protected void initServConf() {
        regConfForProvider = new RegistryConfig();
        regConfForService = new RegistryConfig();
        provConf = new ProviderConfig();
        protoConfForProvider = new ProtocolConfig("mockprotocol");
        protoConfForService = new ProtocolConfig("mockprotocol");
        methodConfForService = new MethodConfig();
        servConf = new ServiceConfig<DemoService>();

//        provConf.setApplication(appConfForProvider);
        application = new ApplicationConfig();
        application.setMetadataServicePort(20881);
        servConf.setApplication(application);

        provConf.setRegistry(regConfForProvider);
        servConf.setRegistry(regConfForService);

        provConf.setProtocols(new ArrayList<>(Arrays.asList(protoConfForProvider)));
        servConf.setProtocols(new ArrayList<>(Arrays.asList(protoConfForService)));

        servConf.setMethods(Arrays.asList(new MethodConfig[]{methodConfForService}));
        servConf.setProvider(provConf);

        servConf.setRef(demoService);
        servConf.setInterfaceClass(DemoService.class);

        methodConfForService.setName("sayName");
        regConfForService.setAddress("127.0.0.1:9090");
        regConfForService.setProtocol("mockregistry");
        application.setName("ConfigTests");
    }

    protected String getProviderParamString() {
        return servConf.getExportedUrls().get(0).toString();
    }

    /**
     * @param paramStringFromDb
     * @param dataTable
     * @param configName
     * @param column
     */
    protected void assertUrlStringWithLocalTable(String paramStringFromDb,
                                                 Object[][] dataTable, String configName, int column) {
        final String FAILLOG_HEADER = "The following config items are not found in URLONE: ";

        log.warn("Verifying service url for " + configName + "... ");
        log.warn("Consumer url string: " + paramStringFromDb);

        String failLog = FAILLOG_HEADER;
        for (Object[] row : dataTable) {

            String targetString = genParamString(row[URL_KEY], row[column]);

            log.warn("Checking " + (String) row[KEY] + "for" + targetString);
            if (paramStringFromDb.contains(targetString)) {
                log.warn((String) row[KEY] + " --> " + targetString + " OK!");
            } else {
                failLog += targetString + ", ";
            }
        }

        if (!failLog.equals(FAILLOG_HEADER)) {
            fail(failLog);
        }
    }

}
