package org.apache.dubbo.errorcode;

import org.apache.dubbo.errorcode.extractor.ErrorCodeExtractor;
import org.apache.dubbo.errorcode.extractor.JavassistConstantPoolErrorCodeExtractor;
import org.apache.dubbo.errorcode.linktest.LinkTestingForkJoinTask;
import org.apache.dubbo.errorcode.util.ErrorUrlUtils;
import org.apache.dubbo.errorcode.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final ForkJoinPool FORK_JOIN_POOL = ForkJoinPool.commonPool();

    public static void main(String[] args) {

        long millis1 = System.currentTimeMillis();

        List<Path> targetFolders = FileUtils.getAllClassFilePaths(args[0]);

        Map<Path, List<String>> fileBasedCodes = new HashMap<>(1024);

        List<String> codes = Collections.synchronizedList(new ArrayList<>(30));

        CountDownLatch countDownLatch = new CountDownLatch(targetFolders.size());

        for (Path folder : targetFolders) {
            EXECUTOR.submit(() -> {
                try (Stream<Path> classFilesStream = Files.walk(folder)) {
                    List<Path> classFiles = classFilesStream.filter(x -> x.toFile().isFile()).collect(Collectors.toList());

                    classFiles.forEach(x -> {

                        List<String> fileBasedCodesErrorCodeList = new ArrayList<String>(4);
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
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        long millis2 = System.currentTimeMillis();
        System.out.println(millis2 - millis1);

        System.out.println("FINAL RESULT: " + codes.stream().distinct().sorted().collect(Collectors.toList()));
        System.out.println(testLinks(codes).entrySet().stream().filter(e -> !e.getValue()).collect(Collectors.toList()));

        EXECUTOR.shutdown();
    }

    private static Map<String, Boolean> testLinks(List<String> codes) {

        List<String> urls = codes.stream().distinct().sorted().map(ErrorUrlUtils::getErrorUrl).collect(Collectors.toList());
        LinkTestingForkJoinTask firstTask = new LinkTestingForkJoinTask(0, urls.size(), urls);

        return FORK_JOIN_POOL.invoke(firstTask);
    }
}
