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

import org.apache.dubbo.errorcode.extractor.ErrorCodeExtractor;
import org.apache.dubbo.errorcode.extractor.JavassistConstantPoolErrorCodeExtractor;
import org.apache.dubbo.errorcode.extractor.MethodDefinition;
import org.apache.dubbo.errorcode.linktest.LinkTestingForkJoinTask;
import org.apache.dubbo.errorcode.reporter.ReportResult;
import org.apache.dubbo.errorcode.reporter.Reporter;
import org.apache.dubbo.errorcode.reporter.impl.ConsoleOutputReporter;
import org.apache.dubbo.errorcode.reporter.impl.FileOutputReporter;
import org.apache.dubbo.errorcode.util.FileUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final List<Class<? extends Reporter>> REPORTER_CLASSES =
        Arrays.asList(ConsoleOutputReporter.class, FileOutputReporter.class);

    private static final List<Reporter> REPORTERS;

    static {
        List<Reporter> tempReporters = new ArrayList<>();

        for (Class<? extends Reporter> cls : REPORTER_CLASSES) {
            try {
                Reporter r = cls.getConstructor().newInstance();

                tempReporters.add(r);

            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        REPORTERS = Collections.unmodifiableList(tempReporters);
    }

    public static void main(String[] args) {

        System.out.println("Directory to inspect: " + args[0]);

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
        System.out.println(millis2 - millis1);

        List<String> linksNotReachable = LinkTestingForkJoinTask.findDocumentMissingErrorCodes(codes);

        ReportResult reportResult = new ReportResult();

        reportResult.setAllErrorCodes(
            codes.stream()
                .distinct()
                .sorted().collect(Collectors.toList()));

        reportResult.setLinkNotReachableErrorCodes(linksNotReachable);

        reportResult.setIllegalInvocations(
            illegalLoggerMethodInvocations.entrySet()
                .stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        REPORTERS.forEach(x -> x.report(reportResult));

        cleanUp();
    }

    private static void handleSinglePackageFolder(Map<Path, List<String>> fileBasedCodes,
                                                  List<String> codes,
                                                  Map<String, List<MethodDefinition>> illegalLoggerMethodInvocation,
                                                  CountDownLatch countDownLatch,
                                                  Path folder) {
        try (Stream<Path> classFilesStream = Files.walk(folder)) {
            List<Path> classFiles = classFilesStream.filter(x -> x.toFile().isFile()).collect(Collectors.toList());

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
