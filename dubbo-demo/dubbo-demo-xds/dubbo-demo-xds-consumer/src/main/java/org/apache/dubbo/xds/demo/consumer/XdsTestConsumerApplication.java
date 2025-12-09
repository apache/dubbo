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
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.xds.demo.DemoService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@SpringBootApplication
@Service
@EnableDubbo
public class XdsTestConsumerApplication {
    private static final Logger logger = LoggerFactory.getLogger(XdsTestConsumerApplication.class);

    @DubboReference(providedBy = "dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local:50051")
    private DemoService demoService;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(XdsTestConsumerApplication.class, args);
        XdsTestConsumerApplication application = context.getBean(XdsTestConsumerApplication.class);
        application.runAllTests();
    }

    public void runAllTests() throws InterruptedException {
        logger.info("=== Starting XDS Strategy Tests ===");

        Thread.sleep(5000);

        // normal test
        normalTest("test");

        // test header matching
        testHeaderRouting();

        // test timeout strategy
        testTimeoutStrategy();

        // test retry strategy
        testRetryStrategy();

        // test weight strategy
        testWeightRouting();

        logger.info("=== All XDS Strategy Tests Completed ===");
    }

    public void normalTest(String name) {
        while (true) {
            try {
                Thread.sleep(10000);
                logger.info("doSayHello called with name: {}", name);
                String result = demoService.sayHello(name);
                logger.info("Call successful, result: {}", result);
            } catch (Exception e) {
                logger.error("Error calling service", e);
            }
        }
    }

    public void testTimeoutStrategy() {
        logger.info("=== Testing Timeout Strategy ===");

        testTimeout("timeout-2s", 2000);

        testTimeout("timeout-5s", 5000);
    }

    private void testTimeout(String scenario, long expectedTimeoutMs) {
        logger.info("Testing timeout scenario: {}, expected timeout: {}ms", scenario, expectedTimeoutMs);

        long startTime = System.currentTimeMillis();
        try {
            RpcContext.getClientAttachment().setAttachment("test-scenario", scenario);

            String result = demoService.sayHello("timeout-test");
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Timeout test [{}] SUCCESS: result={}, duration={}ms", scenario, result, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Timeout test [{}] TIMEOUT: duration={}ms, error={}", scenario, duration, e.getMessage());

            if (duration >= expectedTimeoutMs - 500 && duration <= expectedTimeoutMs + 1000) {
                logger.info(
                        "✅ Timeout strategy WORKING: actual timeout {}ms matches expected {}ms",
                        duration,
                        expectedTimeoutMs);
            } else {
                logger.warn(
                        "❌ Timeout strategy NOT WORKING: actual timeout {}ms, expected {}ms",
                        duration,
                        expectedTimeoutMs);
            }
        } finally {
            RpcContext.getClientAttachment().clearAttachments();
        }
    }

    public void testRetryStrategy() {
        logger.info("=== Testing Retry Strategy ===");

        AtomicInteger attemptCount = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            long startTime = System.currentTimeMillis();
            try {
                RpcContext.getClientAttachment().setAttachment("test-scenario", "retry-test");
                RpcContext.getClientAttachment().setAttachment("request-id", "retry-test-" + i);
                RpcContext.getClientAttachment()
                        .setAttachment("attempt-id", String.valueOf(attemptCount.incrementAndGet()));

                String result = demoService.sayHello("retry-test-" + i);
                long duration = System.currentTimeMillis() - startTime;

                logger.info("Retry test [{}] SUCCESS: result={}, duration={}ms", i, result, duration);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Retry test [{}] FAILED: duration={}ms, error={}", i, duration, e.getMessage());

                if (duration > 2000) {
                    logger.info("✅ Retry strategy WORKING: duration {}ms suggests retries occurred", duration);
                } else {
                    logger.warn("❌ Retry strategy might NOT be working: duration {}ms too short for retries", duration);
                }
            } finally {
                RpcContext.getClientAttachment().clearAttachments();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void testWeightRouting() {
        logger.info("=== Testing Weight Routing Strategy ===");

        Map<String, AtomicInteger> versionCounts = new HashMap<>();
        versionCounts.put("v1", new AtomicInteger(0));
        versionCounts.put("v2", new AtomicInteger(0));
        versionCounts.put("unknown", new AtomicInteger(0));

        int totalRequests = 100;
        CompletableFuture<Void>[] futures = new CompletableFuture[totalRequests];

        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            futures[i] = CompletableFuture.runAsync(
                    () -> {
                        try {
                            RpcContext.getClientAttachment().setAttachment("test-scenario", "weight-test");
                            RpcContext.getClientAttachment().setAttachment("request-id", String.valueOf(requestId));

                            String result = demoService.sayHello("weight-test-" + requestId);

                            if (result.contains("v1")) {
                                versionCounts.get("v1").incrementAndGet();
                            } else if (result.contains("v2")) {
                                versionCounts.get("v2").incrementAndGet();
                            } else {
                                versionCounts.get("unknown").incrementAndGet();
                            }

                            if (requestId % 20 == 0) {
                                logger.info(
                                        "Weight test progress: {}/{}, result: {}",
                                        requestId + 1,
                                        totalRequests,
                                        result);
                            }

                        } catch (Exception e) {
                            logger.warn("Weight test request {} failed: {}", requestId, e.getMessage());
                        } finally {
                            RpcContext.getClientAttachment().clearAttachments();
                        }
                    },
                    executor);
        }

        CompletableFuture.allOf(futures).join();

        int v1Count = versionCounts.get("v1").get();
        int v2Count = versionCounts.get("v2").get();
        int unknownCount = versionCounts.get("unknown").get();

        double v1Percentage = (double) v1Count / totalRequests * 100;
        double v2Percentage = (double) v2Count / totalRequests * 100;

        logger.info("Weight routing results:");
        logger.info("  v1: {} requests ({}%)", v1Count, String.format("%.1f", v1Percentage));
        logger.info("  v2: {} requests ({}%)", v2Count, String.format("%.1f", v2Percentage));
        logger.info("  unknown: {} requests", unknownCount);

        if (Math.abs(v1Percentage - 70.0) < 15.0 && Math.abs(v2Percentage - 30.0) < 15.0) {
            logger.info(
                    "✅ Weight routing strategy WORKING: v1={}%, v2={}% (expected v1=70%, v2=30%)",
                    String.format("%.1f", v1Percentage), String.format("%.1f", v2Percentage));
        } else {
            logger.warn(
                    "❌ Weight routing strategy NOT WORKING: v1={}%, v2={}% (expected v1=70%, v2=30%)",
                    String.format("%.1f", v1Percentage), String.format("%.1f", v2Percentage));
        }
    }

    /**
     * Test header-based routing strategy
     */
    public void testHeaderRouting() {
        logger.info("=== Testing Header-based Routing Strategy ===");

        // Test VIP user -> should route to v2
        testSingleHeaderRouting("user-type", "vip", "v2", "VIP User");

        // Test normal user -> should route to v1
        testSingleHeaderRouting("user-type", "normal", "v1", "Normal User");

        // Test no user-type header -> should distribute randomly
        testNoHeaderRouting("No User Type Header");
    }

    /**
     * Test single header routing
     */
    private void testSingleHeaderRouting(
            String headerName, String headerValue, String expectedVersion, String testName) {
        Map<String, AtomicInteger> versionCounts = new HashMap<>();
        versionCounts.put("v1", new AtomicInteger(0));
        versionCounts.put("v2", new AtomicInteger(0));
        versionCounts.put("unknown", new AtomicInteger(0));

        int testRequests = 20;

        for (int i = 0; i < testRequests; i++) {
            try {
                // Set header
                RpcContext.getClientAttachment().setAttachment(headerName, headerValue);
                RpcContext.getClientAttachment().setAttachment("test-scenario", "header-routing");

                String result = demoService.sayHello("header-test-" + i);

                // Parse version information
                String actualVersion = extractVersionFromResult(result);
                versionCounts.get(actualVersion).incrementAndGet();

                if (i == 0) {
                    logger.info("{} test: {}={} -> result: {}", testName, headerName, headerValue, result);
                }

            } catch (Exception e) {
                logger.warn("{} test failed: {}", testName, e.getMessage());
                versionCounts.get("unknown").incrementAndGet();
            } finally {
                RpcContext.getClientAttachment().clearAttachments();
            }
        }

        // Analyze results
        analyzeHeaderRoutingResult(testName, versionCounts, expectedVersion, testRequests);
    }

    /**
     * Test routing without headers (default routing)
     */
    private void testNoHeaderRouting(String testName) {
        Map<String, AtomicInteger> versionCounts = new HashMap<>();
        versionCounts.put("v1", new AtomicInteger(0));
        versionCounts.put("v2", new AtomicInteger(0));
        versionCounts.put("unknown", new AtomicInteger(0));

        int testRequests = 20;

        for (int i = 0; i < testRequests; i++) {
            try {
                RpcContext.getClientAttachment().setAttachment("test-scenario", "header-routing");

                String result = demoService.sayHello("header-test-" + i);

                // 解析版本信息
                String actualVersion = extractVersionFromResult(result);
                versionCounts.get(actualVersion).incrementAndGet();

                if (i == 0) {
                    logger.info("{} test: -> result: {}", testName, result);
                }

            } catch (Exception e) {
                logger.warn("{} test failed: {}", testName, e.getMessage());
                versionCounts.get("unknown").incrementAndGet();
            } finally {
                RpcContext.getClientAttachment().clearAttachments();
            }
        }

        // Analyze default routing results (should be 50/50 distribution)
        int v1Count = versionCounts.get("v1").get();
        int v2Count = versionCounts.get("v2").get();
        double v1Percentage = (double) v1Count / testRequests * 100;
        double v2Percentage = (double) v2Count / testRequests * 100;

        logger.info(
                "{} result: v1={}times({}%), v2={}times({}%)",
                testName, v1Count, String.format("%.1f", v1Percentage), v2Count, String.format("%.1f", v2Percentage));

        if (v1Count > 0 && v2Count > 0) {
            logger.info("✅ {} strategy working: traffic distributed between v1 and v2", testName);
        } else {
            logger.warn("❌ {} strategy failed: traffic not properly distributed", testName);
        }
    }

    /**
     * Extract version information from result
     */
    private String extractVersionFromResult(String result) {
        if (result.contains("from v1")) {
            return "v1";
        } else if (result.contains("from v2")) {
            return "v2";
        } else {
            return "unknown";
        }
    }

    /**
     * Analyze header routing results
     */
    private void analyzeHeaderRoutingResult(
            String testName, Map<String, AtomicInteger> versionCounts, String expectedVersion, int totalRequests) {
        int v1Count = versionCounts.get("v1").get();
        int v2Count = versionCounts.get("v2").get();
        int unknownCount = versionCounts.get("unknown").get();

        double v1Percentage = (double) v1Count / totalRequests * 100;
        double v2Percentage = (double) v2Count / totalRequests * 100;

        logger.info(
                "{} result: v1={}times({}%), v2={}times({}%), unknown={}times",
                testName,
                v1Count,
                String.format("%.1f", v1Percentage),
                v2Count,
                String.format("%.1f", v2Percentage),
                unknownCount);

        // Verify if it meets expectations
        if ("v1".equals(expectedVersion)) {
            if (v1Percentage >= 80.0) {
                logger.info(
                        "✅ {} strategy working: {}% traffic routed to v1",
                        testName, String.format("%.1f", v1Percentage));
            } else {
                logger.warn(
                        "❌ {} strategy failed: expected routing to v1, but only {}% traffic",
                        testName, String.format("%.1f", v1Percentage));
            }
        } else if ("v2".equals(expectedVersion)) {
            if (v2Percentage >= 80.0) {
                logger.info(
                        "✅ {} strategy working: {}% traffic routed to v2",
                        testName, String.format("%.1f", v2Percentage));
            } else {
                logger.warn(
                        "❌ {} strategy failed: expected routing to v2, but only {}% traffic",
                        testName, String.format("%.1f", v2Percentage));
            }
        }
    }

    /**
     * Test Dubbo method routing
     * This relies on XdsRouter to automatically extract the method path and match VirtualService rules
     */
    private void testDubboMethodRouting(String methodName, String testName, boolean isWeighted) {
        Map<String, AtomicInteger> versionCounts = new HashMap<>();
        versionCounts.put("v1", new AtomicInteger(0));
        versionCounts.put("v2", new AtomicInteger(0));
        versionCounts.put("unknown", new AtomicInteger(0));

        int testRequests = isWeighted ? 100 : 20; // More requests for weighted analysis

        for (int i = 0; i < testRequests; i++) {
            try {
                // Set test scenario to enable path routing
                RpcContext.getClientAttachment().setAttachment("test-scenario", "path-routing");

                String result;
                // Call the actual Dubbo method - XdsRouter will extract the path automatically
                if ("sayHello".equals(methodName)) {
                    result = demoService.sayHello("path-test-" + i);
                } else {
                    // For other methods, we would call them here
                    result = demoService.sayHello("path-test-" + i);
                }

                // Parse version information
                String actualVersion = extractVersionFromResult(result);
                versionCounts.get(actualVersion).incrementAndGet();

                if (i == 0) {
                    logger.info("{} test: method={} -> result: {}", testName, methodName, result);
                }

            } catch (Exception e) {
                logger.warn("{} test failed: {}", testName, e.getMessage());
                versionCounts.get("unknown").incrementAndGet();
            } finally {
                RpcContext.getClientAttachment().clearAttachments();
            }
        }

        // Analyze results
        if (isWeighted) {
            analyzeWeightedPathRoutingResult(testName, versionCounts, testRequests);
        } else {
            // For non-weighted routing, we would specify expected version
            analyzePathRoutingResult(testName, versionCounts, "v1", testRequests);
        }
    }

    /**
     * Analyze path routing results
     */
    private void analyzePathRoutingResult(
            String testName, Map<String, AtomicInteger> versionCounts, String expectedVersion, int totalRequests) {
        int v1Count = versionCounts.get("v1").get();
        int v2Count = versionCounts.get("v2").get();
        int unknownCount = versionCounts.get("unknown").get();

        double v1Percentage = (double) v1Count / totalRequests * 100;
        double v2Percentage = (double) v2Count / totalRequests * 100;

        logger.info(
                "{} result: v1={}times({}%), v2={}times({}%), unknown={}times",
                testName,
                v1Count,
                String.format("%.1f", v1Percentage),
                v2Count,
                String.format("%.1f", v2Percentage),
                unknownCount);

        // Verify if it meets expectations
        if ("v1".equals(expectedVersion)) {
            if (v1Percentage >= 80.0) {
                logger.info(
                        "✅ {} strategy working: {}% traffic routed to v1",
                        testName, String.format("%.1f", v1Percentage));
            } else {
                logger.warn(
                        "❌ {} strategy failed: expected routing to v1, but only {}% traffic",
                        testName, String.format("%.1f", v1Percentage));
            }
        } else if ("v2".equals(expectedVersion)) {
            if (v2Percentage >= 80.0) {
                logger.info(
                        "✅ {} strategy working: {}% traffic routed to v2",
                        testName, String.format("%.1f", v2Percentage));
            } else {
                logger.warn(
                        "❌ {} strategy failed: expected routing to v2, but only {}% traffic",
                        testName, String.format("%.1f", v2Percentage));
            }
        }
    }

    /**
     * Analyze weighted path routing results
     */
    private void analyzeWeightedPathRoutingResult(
            String testName, Map<String, AtomicInteger> versionCounts, int totalRequests) {
        int v1Count = versionCounts.get("v1").get();
        int v2Count = versionCounts.get("v2").get();
        int unknownCount = versionCounts.get("unknown").get();

        double v1Percentage = (double) v1Count / totalRequests * 100;
        double v2Percentage = (double) v2Count / totalRequests * 100;

        logger.info(
                "{} result: v1={}times({}%), v2={}times({}%), unknown={}times",
                testName,
                v1Count,
                String.format("%.1f", v1Percentage),
                v2Count,
                String.format("%.1f", v2Percentage),
                unknownCount);

        // Expected: 70% v1, 30% v2 (with ±10% tolerance)
        boolean v1InRange = v1Percentage >= 60.0 && v1Percentage <= 80.0;
        boolean v2InRange = v2Percentage >= 20.0 && v2Percentage <= 40.0;

        if (v1InRange && v2InRange) {
            logger.info(
                    "✅ {} weighted routing working: v1={}%, v2={}% (expected 70%/30%)",
                    testName, String.format("%.1f", v1Percentage), String.format("%.1f", v2Percentage));
        } else {
            logger.warn(
                    "❌ {} weighted routing failed: v1={}%, v2={}% (expected ~70%/30%)",
                    testName, String.format("%.1f", v1Percentage), String.format("%.1f", v2Percentage));
        }
    }
}
