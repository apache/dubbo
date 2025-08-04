///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.apache.dubbo.xds.demo.consumer;
//
//import org.apache.dubbo.config.annotation.DubboReference;
//import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
//import org.apache.dubbo.rpc.RpcContext;
//import org.apache.dubbo.xds.demo.DemoService;
//
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.stereotype.Service;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@SpringBootApplication
//@Service
//@EnableDubbo
//public class XdsTestConsumerApplication {
//    private static final Logger logger = LoggerFactory.getLogger(XdsTestConsumerApplication.class);
//
//    @DubboReference(providedBy = "dubbo-demo-xds-provider.dubbo-proxyless.svc.cluster.local:50051")
//    private DemoService demoService;
//
//    private final ExecutorService executor = Executors.newFixedThreadPool(10);
//
//    public static void main(String[] args) throws InterruptedException {
//        ConfigurableApplicationContext context = SpringApplication.run(XdsTestConsumerApplication.class, args);
//        XdsTestConsumerApplication application = context.getBean(XdsTestConsumerApplication.class);
//
//        // 运行所有测试
//        application.runAllTests();
//    }
//
//    public void runAllTests() throws InterruptedException {
//        logger.info("=== Starting XDS Strategy Tests ===");
//
//        // 1. 测试超时策略
//        testTimeoutStrategy();
//        Thread.sleep(5000);
//
//        // 2. 测试重试策略
//        testRetryStrategy();
//        Thread.sleep(5000);
//
//        // 3. 测试权重路由
//        testWeightRouting();
//        Thread.sleep(5000);
//
//        // 4. 测试一致性哈希
//        testHashRouting();
//        Thread.sleep(5000);
//
//        logger.info("=== All XDS Strategy Tests Completed ===");
//    }
//
//    /**
//     * 测试超时策略
//     */
//    public void testTimeoutStrategy() {
//        logger.info("=== Testing Timeout Strategy ===");
//
//        // 测试2秒超时
//        testTimeout("timeout-2s", 2000);
//
//        // 测试5秒超时
//        testTimeout("timeout-5s", 5000);
//    }
//
//    private void testTimeout(String scenario, long expectedTimeoutMs) {
//        logger.info("Testing timeout scenario: {}, expected timeout: {}ms", scenario, expectedTimeoutMs);
//
//        long startTime = System.currentTimeMillis();
//        try {
//            // 设置测试场景header
//            RpcContext.getClientAttachment().setAttachment("test-scenario", scenario);
//
//            String result = demoService.sayHello("timeout-test");
//            long duration = System.currentTimeMillis() - startTime;
//
//            logger.info("Timeout test [{}] SUCCESS: result={}, duration={}ms", scenario, result, duration);
//
//        } catch (Exception e) {
//            long duration = System.currentTimeMillis() - startTime;
//            logger.info("Timeout test [{}] TIMEOUT: duration={}ms, error={}", scenario, duration, e.getMessage());
//
//            // 验证超时时间是否符合预期
//            if (duration >= expectedTimeoutMs - 500 && duration <= expectedTimeoutMs + 1000) {
//                logger.info("✅ Timeout strategy WORKING: actual timeout {}ms matches expected {}ms", duration, expectedTimeoutMs);
//            } else {
//                logger.warn("❌ Timeout strategy NOT WORKING: actual timeout {}ms, expected {}ms", duration, expectedTimeoutMs);
//            }
//        } finally {
//            RpcContext.getClientAttachment().clearAttachments();
//        }
//    }
//
//    /**
//     * 测试重试策略
//     */
//    public void testRetryStrategy() {
//        logger.info("=== Testing Retry Strategy ===");
//
//        AtomicInteger attemptCount = new AtomicInteger(0);
//
//        for (int i = 0; i < 5; i++) {
//            long startTime = System.currentTimeMillis();
//            try {
//                RpcContext.getClientAttachment().setAttachment("test-scenario", "retry-test");
//                RpcContext.getClientAttachment().setAttachment("attempt-id", String.valueOf(attemptCount.incrementAndGet()));
//
//                String result = demoService.sayHello("retry-test-" + i);
//                long duration = System.currentTimeMillis() - startTime;
//
//                logger.info("Retry test [{}] SUCCESS: result={}, duration={}ms", i, result, duration);
//
//            } catch (Exception e) {
//                long duration = System.currentTimeMillis() - startTime;
//                logger.info("Retry test [{}] FAILED: duration={}ms, error={}", i, duration, e.getMessage());
//
//                // 检查是否进行了重试（通过时间判断）
//                if (duration > 2000) { // 如果超过2秒，说明可能进行了重试
//                    logger.info("✅ Retry strategy WORKING: duration {}ms suggests retries occurred", duration);
//                } else {
//                    logger.warn("❌ Retry strategy might NOT be working: duration {}ms too short for retries", duration);
//                }
//            } finally {
//                RpcContext.getClientAttachment().clearAttachments();
//            }
//
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                break;
//            }
//        }
//    }
//
//    /**
//     * 测试权重路由
//     */
//    public void testWeightRouting() {
//        logger.info("=== Testing Weight Routing Strategy ===");
//
//        Map<String, AtomicInteger> versionCounts = new HashMap<>();
//        versionCounts.put("v1", new AtomicInteger(0));
//        versionCounts.put("v2", new AtomicInteger(0));
//        versionCounts.put("unknown", new AtomicInteger(0));
//
//        int totalRequests = 100;
//        CompletableFuture<Void>[] futures = new CompletableFuture[totalRequests];
//
//        for (int i = 0; i < totalRequests; i++) {
//            final int requestId = i;
//            futures[i] = CompletableFuture.runAsync(() -> {
//                try {
//                    RpcContext.getClientAttachment().setAttachment("test-scenario", "weight-test");
//                    RpcContext.getClientAttachment().setAttachment("request-id", String.valueOf(requestId));
//
//                    String result = demoService.sayHello("weight-test-" + requestId);
//
//                    // 解析响应中的版本信息
//                    if (result.contains("v1")) {
//                        versionCounts.get("v1").incrementAndGet();
//                    } else if (result.contains("v2")) {
//                        versionCounts.get("v2").incrementAndGet();
//                    } else {
//                        versionCounts.get("unknown").incrementAndGet();
//                    }
//
//                    if (requestId % 20 == 0) {
//                        logger.info("Weight test progress: {}/{}, result: {}", requestId + 1, totalRequests, result);
//                    }
//
//                } catch (Exception e) {
//                    logger.warn("Weight test request {} failed: {}", requestId, e.getMessage());
//                } finally {
//                    RpcContext.getClientAttachment().clearAttachments();
//                }
//            }, executor);
//        }
//
//        // 等待所有请求完成
//        CompletableFuture.allOf(futures).join();
//
//        // 分析结果
//        int v1Count = versionCounts.get("v1").get();
//        int v2Count = versionCounts.get("v2").get();
//        int unknownCount = versionCounts.get("unknown").get();
//
//        double v1Percentage = (double) v1Count / totalRequests * 100;
//        double v2Percentage = (double) v2Count / totalRequests * 100;
//
//        logger.info("Weight routing results:");
//        logger.info("  v1: {} requests ({}%)", v1Count, String.format("%.1f", v1Percentage));
//        logger.info("  v2: {} requests ({}%)", v2Count, String.format("%.1f", v2Percentage));
//        logger.info("  unknown: {} requests", unknownCount);
//
//        // 验证权重分配（期望v1:70%, v2:30%）
//        if (Math.abs(v1Percentage - 70.0) < 15.0 && Math.abs(v2Percentage - 30.0) < 15.0) {
//            logger.info("✅ Weight routing strategy WORKING: v1={}%, v2={}% (expected v1=70%, v2=30%)",
//                String.format("%.1f", v1Percentage), String.format("%.1f", v2Percentage));
//        } else {
//            logger.warn("❌ Weight routing strategy NOT WORKING: v1={}%, v2={}% (expected v1=70%, v2=30%)",
//                String.format("%.1f", v1Percentage), String.format("%.1f", v2Percentage));
//        }
//    }
//
//    /**
//     * 测试一致性哈希路由
//     */
//    public void testHashRouting() {
//        logger.info("=== Testing Hash Routing Strategy ===");
//
//        Map<String, String> userToVersion = new HashMap<>();
//        String[] userIds = {"user1", "user2", "user3", "user4", "user5"};
//
//        // 每个用户发送多次请求，验证是否路由到同一个版本
//        for (String userId : userIds) {
//            String firstVersion = null;
//            boolean consistent = true;
//
//            for (int i = 0; i < 10; i++) {
//                try {
//                    RpcContext.getClientAttachment().setAttachment("test-scenario", "hash-test");
//                    RpcContext.getClientAttachment().setAttachment("user-id", userId);
//
//                    String result = demoService.sayHello("hash-test-" + userId + "-" + i);
//
//                    String version = "unknown";
//                    if (result.contains("v1")) {
//                        version = "v1";
//                    } else if (result.contains("v2")) {
//                        version = "v2";
//                    }
//
//                    if (firstVersion == null) {
//                        firstVersion = version;
//                        userToVersion.put(userId, version);
//                    } else if (!firstVersion.equals(version)) {
//                        consistent = false;
//                        logger.warn("Hash routing inconsistency for {}: first={}, current={}", userId, firstVersion, version);
//                    }
//
//                    if (i == 0) {
//                        logger.info("Hash test user {} -> version {}, result: {}", userId, version, result);
//                    }
//
//                } catch (Exception e) {
//                    logger.warn("Hash test failed for user {}: {}", userId, e.getMessage());
//                } finally {
//                    RpcContext.getClientAttachment().clearAttachments();
//                }
//            }
//
//            if (consistent) {
//                logger.info("✅ User {} consistently routed to version {}", userId, firstVersion);
//            } else {
//                logger.warn("❌ User {} routing is NOT consistent", userId);
//            }
//        }
//
//        // 验证不同用户是否可能路由到不同版本
//        long distinctVersions = userToVersion.values().stream().distinct().count();
//        if (distinctVersions > 1) {
//            logger.info("✅ Hash routing strategy WORKING: {} different versions used across users", distinctVersions);
//        } else {
//            logger.warn("❌ Hash routing strategy might NOT be working: all users routed to same version");
//        }
//
//        logger.info("Hash routing summary: {}", userToVersion);
//    }
//}
