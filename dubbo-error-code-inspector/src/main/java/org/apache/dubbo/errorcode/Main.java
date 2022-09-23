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

package org.apache.dubbo.errorcode;

import org.apache.dubbo.errorcode.config.ErrorCodeInspectorConfig;
import org.apache.dubbo.errorcode.extractor.ErrorCodeExtractor;
import org.apache.dubbo.errorcode.extractor.InvalidLoggerInvocationLocator;
import org.apache.dubbo.errorcode.extractor.JavassistConstantPoolErrorCodeExtractor;
import org.apache.dubbo.errorcode.extractor.JdtBasedInvalidLoggerInvocationLocator;
import org.apache.dubbo.errorcode.linktest.LinkTestingForkJoinTask;
import org.apache.dubbo.errorcode.model.LoggerMethodInvocation;
import org.apache.dubbo.errorcode.model.MethodDefinition;
import org.apache.dubbo.errorcode.reporter.InspectionResult;
import org.apache.dubbo.errorcode.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Error code extractor main class.
 */
public class Main {

    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(16, 32, 2, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

    private static final ErrorCodeExtractor ERROR_CODE_EXTRACTOR = new JavassistConstantPoolErrorCodeExtractor();

    private static final InvalidLoggerInvocationLocator INVALID_LOGGER_INVOCATION_LOCATOR = new JdtBasedInvalidLoggerInvocationLocator();

    private static String directoryToInspect;

    public static void main(String[] args) {

        directoryToInspect = args[0];
        System.out.println("Directory to inspect: " + directoryToInspect);

        // Step 1
        System.out.println("Scanning error codes and detecting invalid logger invocation...");

        long millis1 = System.currentTimeMillis();

        List<Path> targetFolders = FileUtils.getAllClassFilePaths(args[0]);
        Map<Path, List<String>> fileBasedCodes = new HashMap<>(1024);
        List<String> codes = Collections.synchronizedList(new ArrayList<>(30));
        Map<String, List<MethodDefinition>> illegalLoggerMethodInvocations = new ConcurrentHashMap<>(256);

        CountDownLatch countDownLatch = new CountDownLatch(targetFolders.size());

        for (Path folder : targetFolders) {
            EXECUTOR.submit(() -> handleSinglePackageFolder(
                fileBasedCodes,
                codes,
                illegalLoggerMethodInvocations,
                countDownLatch,
                folder));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        long millis2 = System.currentTimeMillis();
        System.out.println("Milliseconds elapsed: " + (millis2 - millis1));

        // Step 2
        System.out.println("Locating illegal logger method invocations...");
        millis1 = System.currentTimeMillis();

        Map<String, List<MethodDefinition>> illegalInvocationClassesAndLoggerMethods = illegalLoggerMethodInvocations.entrySet()
            .stream()
            .filter(e -> !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, List<LoggerMethodInvocation>> invalidLoggerMethodInvocationLocations = new HashMap<>();

        Set<String> illegalInvocationClasses = illegalInvocationClassesAndLoggerMethods.keySet();
        illegalInvocationClasses.forEach(x ->
            invalidLoggerMethodInvocationLocations.put(
                x, INVALID_LOGGER_INVOCATION_LOCATOR.locateInvalidLoggerInvocation(x)
            )
        );

        millis2 = System.currentTimeMillis();
        System.out.println("Milliseconds elapsed: " + (millis2 - millis1));

        // Step 3
        System.out.println("Finding error codes that document links are not reachable...");
        millis1 = System.currentTimeMillis();

        List<String> linksNotReachable = LinkTestingForkJoinTask.findDocumentMissingErrorCodes(codes);

        millis2 = System.currentTimeMillis();
        System.out.println("Milliseconds elapsed: " + (millis2 - millis1));
        System.out.println();

        InspectionResult inspectionResult = getInspectionResult(
            codes,
            illegalInvocationClassesAndLoggerMethods,
            invalidLoggerMethodInvocationLocations,
            linksNotReachable);

        ErrorCodeInspectorConfig.REPORTERS.forEach(x -> x.report(inspectionResult));

        cleanUp();

        if (ErrorCodeInspectorConfig.REPORT_AS_ERROR) {
            if (!inspectionResult.getIllegalInvocations().isEmpty() ||
                !inspectionResult.getLinkNotReachableErrorCodes().isEmpty()) {

                throw new IllegalStateException("Invalid situation occurred, check console or log for details;");
            }
        } else {
            System.out.println("Tolerance mode enabled, will not throw exception.");
        }
    }

    private static InspectionResult getInspectionResult(List<String> codes, Map<String, List<MethodDefinition>> illegalInvocationClassesAndLoggerMethods, Map<String, List<LoggerMethodInvocation>> invalidLoggerMethodInvocationLocations, List<String> linksNotReachable) {
        InspectionResult inspectionResult = new InspectionResult();

        inspectionResult.setAllErrorCodes(
            codes.stream()
                .distinct()
                .sorted().collect(Collectors.toList()));

        inspectionResult.setLinkNotReachableErrorCodes(linksNotReachable);
        inspectionResult.setIllegalInvocations(illegalInvocationClassesAndLoggerMethods);
        inspectionResult.setInvalidLoggerMethodInvocationLocations(invalidLoggerMethodInvocationLocations);

        return inspectionResult;
    }

    private static void handleSinglePackageFolder(Map<Path, List<String>> fileBasedCodes,
                                                  List<String> codes,
                                                  Map<String, List<MethodDefinition>> illegalLoggerMethodInvocation,
                                                  CountDownLatch countDownLatch,
                                                  Path folder) {

        try (Stream<Path> classFilesStream = Files.walk(folder)) {

            List<Path> classFiles = classFilesStream
                .filter(x -> x.toFile().isFile())
                .filter(x -> !ErrorCodeInspectorConfig.EXCLUSIONS.contains(Paths.get(directoryToInspect).relativize(x).toString()))
                .collect(Collectors.toList());

            classFiles.forEach(x -> {

                List<String> fileBasedCodesErrorCodeList = new ArrayList<>(4);
                List<String> extractedCodeList = ERROR_CODE_EXTRACTOR.getErrorCodes(x.toString());
                illegalLoggerMethodInvocation.put(x.toString(), ERROR_CODE_EXTRACTOR.getIllegalLoggerMethodInvocations(x.toString()));

                if (!extractedCodeList.isEmpty()) {
                    fileBasedCodesErrorCodeList.addAll(extractedCodeList.stream().distinct().collect(Collectors.toList()));
                    fileBasedCodes.put(x, fileBasedCodesErrorCodeList);

                    codes.addAll(extractedCodeList);
                }
            });

        } catch (IOException ignored) {
            // ignored.
        } finally {
            countDownLatch.countDown();
        }
    }

    private static void cleanUp() {
        EXECUTOR.shutdown();
        LinkTestingForkJoinTask.closeHttpClient();
    }
}
