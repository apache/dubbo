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
import org.apache.dubbo.errorcode.linktest.LinkTestingForkJoinTask;
import org.apache.dubbo.errorcode.util.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
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

    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    public static void main(String[] args) {

        long millis1 = System.currentTimeMillis();

        List<Path> targetFolders = FileUtils.getAllClassFilePaths(args[0]);
        Map<Path, List<String>> fileBasedCodes = new HashMap<>(1024);
        List<String> codes = Collections.synchronizedList(new ArrayList<>(30));

        CountDownLatch countDownLatch = new CountDownLatch(targetFolders.size());

        for (Path folder : targetFolders) {
            EXECUTOR.submit(() -> handleSinglePackageFolder(fileBasedCodes, codes, countDownLatch, folder));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        long millis2 = System.currentTimeMillis();
        System.out.println(millis2 - millis1);

        System.out.println("All error codes: " + codes.stream().distinct().sorted().collect(Collectors.toList()));

        List<String> linksNotReachable = LinkTestingForkJoinTask.findDocumentMissingErrorCodes(codes);
        System.out.println("Error codes which document links are not reachable: " + linksNotReachable);

        try (PrintStream printStream = new PrintStream(Files.newOutputStream(Paths.get(System.getProperty("user.dir"), "error-inspection-result.txt")))) {

            printStream.println("All error codes: " + codes.stream().distinct().sorted().collect(Collectors.toList()));
            printStream.println();
            printStream.println("Error codes which document links are not reachable: " + linksNotReachable);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        EXECUTOR.shutdown();
        LinkTestingForkJoinTask.closeHttpClient();
        FORK_JOIN_POOL.shutdown();
    }

    private static void handleSinglePackageFolder(Map<Path, List<String>> fileBasedCodes, List<String> codes, CountDownLatch countDownLatch, Path folder) {
        try (Stream<Path> classFilesStream = Files.walk(folder)) {
            List<Path> classFiles = classFilesStream.filter(x -> x.toFile().isFile()).collect(Collectors.toList());

            classFiles.forEach(x -> {

                List<String> fileBasedCodesErrorCodeList = new ArrayList<>(4);
                List<String> extractedCodeList = ERROR_CODE_EXTRACTOR.getErrorCodes(x.toString());

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
}
