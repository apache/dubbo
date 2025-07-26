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
package org.apache.dubbo.xds.demo.consumer;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.xds.demo.DemoService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Service
@EnableDubbo
public class XdsConsumerApplication {
    private static final Logger logger = LoggerFactory.getLogger(XdsConsumerApplication.class);

    @DubboReference(providedBy = "dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local")
    private DemoService demoService;

    public static void main(String[] args) throws InterruptedException {
        // System.setProperty(IstioConstant.WORKLOAD_NAMESPACE_KEY, "dubbo-demo");
        // // System.setProperty("API_SERVER_PATH", "https://127.0.0.1:6443");
        // System.setProperty("SA_CA_PATH", "/Users/smzdm/hjf/xds/resources/ca.crt");
        // System.setProperty("SA_TOKEN_PATH", "/Users/smzdm/hjf/xds/resources/token");
        // System.setProperty("NAMESPACE", "dubbo-demo");
        // IstioConstant.KUBERNETES_SA_PATH = "/Users/smzdm/hjf/xds/resources/token";
        // System.setProperty(IstioConstant.PILOT_CERT_PROVIDER_KEY, "istiod");

        // System.setProperty("GRPC_XDS_BOOTSTRAP",
        // "/Users/hejianfei/code/server/dubbo/dubbo-demo/dubbo-demo-xds/dubbo-demo-xds-consumer/src/main/resources/bootstrap.json");

        ConfigurableApplicationContext context = SpringApplication.run(XdsConsumerApplication.class, args);
        XdsConsumerApplication application = context.getBean(XdsConsumerApplication.class);

        logger.info("Sleeping for 10 seconds before starting service calls...");
        Thread.sleep(10000);
        logger.info("Starting weighted routing test with 10 consecutive calls...");

        // 测试加权路由分布 - 连续10次调用以获得更准确的统计
        Map<String, Integer> versionCount = new HashMap<>();
        int totalCalls = 100;

        logger.info("=== Starting Weighted Routing Test ===");
        logger.info("Expected distribution: v1 (~20%) vs v2 (~80%)");
        logger.info("Making {} consecutive calls...", totalCalls);

        for (int i = 1; i <= totalCalls; i++) {
            try {
                logger.info("Call {}/{}: Attempting to call demoService.sayHello...", i, totalCalls);
                String result = application.doSayHello("test-" + i);
                logger.info("Call {}/{}: SUCCESS - Result: '{}'", i, totalCalls, result);

                // 分析结果中的版本信息
                String version = "unknown";
                if (result != null) {
                    if (result.contains(" from v1")) {
                        version = "v1";
                    } else if (result.contains(" from v2")) {
                        version = "v2";
                    } else {
                        // 记录未识别的响应格式
                        logger.warn("Could not identify version from response: '{}'", result);
                        version = "unknown";
                    }
                }

                versionCount.put(version, versionCount.getOrDefault(version, 0) + 1);

            } catch (Exception e) {
                logger.error("Call {}/{}: FAILED - Error calling service", i, totalCalls, e);
                versionCount.put("failed", versionCount.getOrDefault("failed", 0) + 1);
            }

            // 增加延迟以便更好地观察路由分布
            Thread.sleep(2000);
        }

        // 输出测试结果统计
        logger.info("=== Weighted Routing Test Results ===");
        logger.info("Total calls made: {}", totalCalls);
        for (Map.Entry<String, Integer> entry : versionCount.entrySet()) {
            String version = entry.getKey();
            int count = entry.getValue();
            double percentage = (count * 100.0) / totalCalls;
            logger.info("Version '{}': {} calls ({}%)", version, count, percentage);
        }

        // 验证分布是否符合预期
        int v1Count = versionCount.getOrDefault("v1", 0);
        int v2Count = versionCount.getOrDefault("v2", 0);
        int unknownCount = versionCount.getOrDefault("unknown", 0);
        int failedCount = versionCount.getOrDefault("failed", 0);

        double v1Percentage = (v1Count * 100.0) / totalCalls;
        double v2Percentage = (v2Count * 100.0) / totalCalls;

        logger.info("=== Distribution Analysis ===");
        logger.info("Expected: v1 ~20%, v2 ~80%");
        logger.info("Actual: v1 {}% ({} calls), v2 {}% ({} calls)",
                   v1Percentage, v1Count, v2Percentage, v2Count);

        if (unknownCount > 0) {
            logger.warn("Unknown responses: {} ({}%)", unknownCount, (unknownCount * 100.0) / totalCalls);
        }
        if (failedCount > 0) {
            logger.error("Failed calls: {} ({}%)", failedCount, (failedCount * 100.0) / totalCalls);
        }

        // 评估路由结果
        if (failedCount == 0 && unknownCount == 0) {
            if (v1Percentage >= 15 && v1Percentage <= 25 && v2Percentage >= 75 && v2Percentage <= 85) {
                logger.info("✅ Weighted routing is working correctly! Distribution is within expected range.");
            } else if (v1Count > 0 && v2Count > 0) {
                logger.warn("⚠️  Weighted routing is working but distribution is off from expected values (v1: {:.1f}%, v2: {:.1f}%)", v1Percentage, v2Percentage);
            } else {
                logger.error("❌ Weighted routing failed - all calls went to only one version");
            }
        } else {
            logger.error("❌ Test incomplete due to failed or unrecognized responses");
        }

        logger.info("=== Test completed, starting regular service call loop ===");

        // 服务调用循环
        while (true) {
            try {
                logger.info("Attempting to call demoService.sayHello...");
                String result = application.doSayHello("world");
                logger.info("Call successful, result: {}", result);
            } catch (Exception e) {
                logger.error("Error calling service", e);
            }
            Thread.sleep(10000);
        }
    }

    public String doSayHello(String name) {
        logger.info("doSayHello called with name: {}", name);
        return demoService.sayHello(name);
    }
}
