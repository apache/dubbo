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
package org.apache.dubbo.xds.demo.provider;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.xds.demo.DemoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

@DubboService
public class TestDemoServiceImpl implements DemoService {

    private static final Logger logger = LoggerFactory.getLogger(TestDemoServiceImpl.class);
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final AtomicInteger retryCounter = new AtomicInteger(0);

    @Override
    public String sayHello(String name) {
        String version = System.getProperty("service.version", "v1");
        String podName = System.getenv("POD_NAME");
        String testScenario = RpcContext.getServerAttachment().getAttachment("test-scenario");
        int requestId = requestCounter.incrementAndGet();
        
        logger.info("Request #{} - Hello {}, scenario: {}, version: {}, pod: {}", 
            requestId, name, testScenario, version, podName);
        
        if (testScenario != null) {
            switch (testScenario) {
                case "timeout-2s":
                    return handleTimeoutTest(name, version, podName, 3000); // 3秒延迟，触发2秒超时
                case "timeout-5s":
                    return handleTimeoutTest(name, version, podName, 1000); // 1秒延迟，不触发5秒超时
                case "retry-test":
                    return handleRetryTest(name, version, podName);
                case "weight-test":
                    return handleWeightTest(name, version, podName);
                case "hash-test":
                    return handleHashTest(name, version, podName);
                default:
                    return handleNormalRequest(name, version, podName);
            }
        }
        
        return handleNormalRequest(name, version, podName);
    }
    
    private String handleTimeoutTest(String name, String version, String podName, long delayMs) {
        logger.info("Timeout test: delaying {}ms for version {}", delayMs, version);
        
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RpcException("Interrupted during timeout test");
        }
        
        String response = String.format("hello %s from %s (timeout test, delayed %dms)", name, version, delayMs);
        if (podName != null) {
            response += " (pod: " + podName + ")";
        }
        
        logger.info("Timeout test completed for version {}", version);
        return response;
    }
    
    private String handleRetryTest(String name, String version, String podName) {
        String attemptId = RpcContext.getServerAttachment().getAttachment("attempt-id");
        int currentRetry = retryCounter.incrementAndGet();
        
        logger.info("Retry test: attempt {}, retry counter: {}, version: {}", attemptId, currentRetry, version);
        
        if (currentRetry % 4 != 0) {
            logger.info("Retry test: simulating failure for attempt {}, version {}", attemptId, version);
            throw new RpcException(RpcException.NETWORK_EXCEPTION,
                "Simulated failure for retry test (attempt: " + attemptId + ", version: " + version + ")");
        }
        
        String response = String.format("hello %s from %s (retry test success after %d attempts)", 
            name, version, currentRetry);
        if (podName != null) {
            response += " (pod: " + podName + ")";
        }
        
        logger.info("Retry test SUCCESS for version {}", version);
        return response;
    }
    
    private String handleWeightTest(String name, String version, String podName) {
        String requestId = RpcContext.getServerAttachment().getAttachment("request-id");
        
        logger.info("Weight test: request {}, version: {}, pod: {}", requestId, version, podName);
        
        String response = String.format("hello %s from %s (weight test)", name, version);
        if (podName != null) {
            response += " (pod: " + podName + ")";
        }
        
        return response;
    }
    
    private String handleHashTest(String name, String version, String podName) {
        String userId = RpcContext.getServerAttachment().getAttachment("user-id");
        
        logger.info("Hash test: user {}, version: {}, pod: {}", userId, version, podName);
        
        String response = String.format("hello %s from %s (hash test, user: %s)", name, version, userId);
        if (podName != null) {
            response += " (pod: " + podName + ")";
        }
        
        return response;
    }
    
    private String handleNormalRequest(String name, String version, String podName) {
        logger.info("Normal request: name {}, version: {}", name, version);
        
        String response = String.format("hello %s from %s", name, version);
        if (podName != null) {
            response += " (pod: " + podName + ")";
        }
        
        return response;
    }
}
