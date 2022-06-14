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
package org.apache.dubbo.test.check;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.apache.commons.lang3.StringUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A test listener to check unclosed threads of test.
 *
 * <pre>
 * Usages:
 *  # enable thread checking
 *  mvn test -DcheckThreads=true
 *
 *  # change thread dump wait time (ms)
 *  mvn test -DcheckThreads=true -DthreadDumpWaitTime=5000
 *
 *  # print test reports of all sub modules to single file
 *  mvn test -DcheckThreads=true -DthreadDumpWaitTime=5000 -DreportFile=/path/test-check-report.txt
 * </pre>
 */
public class DubboTestChecker implements TestExecutionListener {

    private static final String CONFIG_CHECK_MODE = "checkMode";
    private static final String CONFIG_CHECK_THREADS = "checkThreads";
    private static final String CONFIG_THREAD_DUMP_WAIT_TIME = "threadDumpWaitTime";
    private static final String CONFIG_FORCE_DESTROY = "forceDestroy";
    private static final String CONFIG_REPORT_FILE = "reportFile";
    private static final String MODE_CLASS = "class";
    private static final String MODE_METHOD = "method";

    private static final Logger logger = LoggerFactory.getLogger(DubboTestChecker.class);

    /**
     * check mode:
     * class - check after class execution finished
     * method - check after method execution finished
     */
    private String checkMode;
    /**
     * whether check unclosed threads
     */
    private boolean checkThreads;
    /**
     * sleep time before dump threads
     */
    private long threadDumpWaitTimeMs;
    /**
     * whether force destroy dubbo engine, default value is true.
     */
    private boolean forceDestroyDubboAfterClass;

    /**
     * Check report file
     */
    private File reportFile;

    /**
     * thread -> stacktrace
     */
    private Map<Thread, StackTraceElement[]> unclosedThreadMap = new ConcurrentHashMap<>();
    // test class name -> thread list
    private Map<String, List<Thread>> unclosedThreadsOfTestMap = new ConcurrentHashMap<>();
    private String identifier;
    private PrintWriter reportWriter;
    private String projectDir;
    private FileOutputStream reportFileOut;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
            init(System.getProperties());
        } catch (IOException e) {
            throw new IllegalStateException("Test checker init failed", e);
        }
    }

    public void init(Properties properties) throws IOException {
        if (properties == null) {
            properties = new Properties();
        }
        // log prefix
        identifier = "[" + this.getClass().getSimpleName() + "] ";

        // checkMode: class/method
        checkMode = StringUtils.lowerCase(properties.getProperty(CONFIG_CHECK_MODE, MODE_CLASS));
        // checkThreads: true/false
        checkThreads = Boolean.parseBoolean(properties.getProperty(CONFIG_CHECK_THREADS, "false"));
        // threadDumpWaitTime
        threadDumpWaitTimeMs = Long.parseLong(properties.getProperty(CONFIG_THREAD_DUMP_WAIT_TIME, "5000"));
        // force destroy dubbo
        forceDestroyDubboAfterClass = Boolean.parseBoolean(properties.getProperty(CONFIG_FORCE_DESTROY, "true"));

        // project dir
        projectDir = new File(".").getCanonicalPath();

        // report file
        String reportFileCanonicalPath = "";
        String defaultReportDir = "target/";
        String defaultReportFileName = "test-check-report.txt";
        if (checkThreads) {
            String reportFilePath = properties.getProperty(CONFIG_REPORT_FILE, defaultReportDir + defaultReportFileName);
            this.reportFile = new File(reportFilePath);
            if (reportFile.isDirectory()) {
                reportFile.mkdirs();
                reportFile = new File(reportFile, defaultReportFileName);
            }
            reportFileOut = new FileOutputStream(this.reportFile);
            reportWriter = new PrintWriter(reportFileOut);
            reportFileCanonicalPath = reportFile.getCanonicalPath();
        }

        log("Project dir: " + projectDir);
        log(String.format("Dubbo test checker configs: checkMode=%s, checkThreads=%s, threadDumpWaitTimeMs=%s, forceDestroy=%s, reportFile=%s",
            checkMode, checkThreads, threadDumpWaitTimeMs, forceDestroyDubboAfterClass, reportFileCanonicalPath));
        flushReportFile();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        // print all unclosed threads
        if (checkThreads) {
            printThreadCheckingSummaryReport();
        } else {
            log("Thread checking is disabled, use -DcheckThreads=true to check unclosed threads.");
        }
        if (reportWriter != null) {
            reportWriter.close();
        }
    }

    private void printThreadCheckingSummaryReport() {
        log("===== Thread Checking Summary Report ======");
        log("Project dir: " + projectDir);
        log("Total found " + unclosedThreadMap.size() + " unclosed threads in " + unclosedThreadsOfTestMap.size() + " tests.");
        log("");
        unclosedThreadsOfTestMap.forEach((testClassName, threads) -> {
            printUnclosedThreads(threads, testClassName);
        });
        flushReportFile();
    }

    private void flushReportFile() {
        try {
            if (reportWriter != null) {
                reportWriter.flush();
            }
            if (reportFileOut != null) {
                reportFileOut.getFD().sync();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Thread, StackTraceElement[]> getUnclosedThreadMap() {
        return unclosedThreadMap;
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        TestSource testSource = testIdentifier.getSource().orElse(null);
        if (testSource instanceof ClassSource) {
//            ClassSource source = (ClassSource) testSource;
//            log("Run test class: " + source.getClassName());
        } else if (testSource instanceof MethodSource) {
            MethodSource source = (MethodSource) testSource;
            log("Run test method: " + source.getClassName() + "#" + source.getMethodName());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {

        TestSource testSource = testIdentifier.getSource().orElse(null);
        String testClassName;
        if (testSource instanceof MethodSource) {
            if (!StringUtils.contains(checkMode, MODE_METHOD)) {
                return;
            }
            MethodSource methodSource = (MethodSource) testSource;
            testClassName = methodSource.getClassName();
            //log("Finish test method: " + methodSource.getClassName() + "#" + methodSource.getMethodName());
        } else if (testSource instanceof ClassSource) {
            if (forceDestroyDubboAfterClass) {
                // make sure destroy dubbo engine
                FrameworkModel.destroyAll();
            }

            if (!StringUtils.contains(checkMode, MODE_CLASS)) {
                return;
            }

            ClassSource source = (ClassSource) testSource;
            testClassName = source.getClassName();
            //log("Finish test class: " + source.getClassName());
        } else {
            return;
        }

        if (checkThreads) {
            checkUnclosedThreads(testClassName, threadDumpWaitTimeMs);
        }
    }

    public Map<Thread, StackTraceElement[]> checkUnclosedThreads(String testClassName, long waitMs) {
        // wait for shutdown
        log("Wait " + waitMs + "ms to check threads of " + testClassName + " ...");
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
        }

        Map<Thread, StackTraceElement[]> threadStacks = Thread.getAllStackTraces();
        List<Thread> unclosedThreads = threadStacks.keySet().stream()
            .filter(thread -> !StringUtils.startsWithAny(thread.getName(),
                "Reference Handler", "Finalizer", "Signal Dispatcher", "Attach Listener", "process reaper", "main" // jvm
                , "surefire-forkedjvm-" // surefire plugin
            ))
            .filter(thread -> !unclosedThreadMap.containsKey(thread))
            .collect(Collectors.toList());
        unclosedThreads.sort(Comparator.comparing(Thread::getName));
        if (unclosedThreads.size() > 0) {
            for (Thread thread : unclosedThreads) {
                unclosedThreadMap.put(thread, threadStacks.get(thread));
            }
            unclosedThreadsOfTestMap.put(testClassName, unclosedThreads);
            printUnclosedThreads(unclosedThreads, testClassName);
        }

        // return new unclosed thread map
        Map<Thread, StackTraceElement[]> unclosedThreadMap = new LinkedHashMap<>();
        for (Thread thread : unclosedThreads) {
            unclosedThreadMap.put(thread, threadStacks.get(thread));
        }
        return unclosedThreadMap;
    }

    private void printUnclosedThreads(List<Thread> threads, String testClassName) {
        if (threads.size() > 0) {
            log("Found " + threads.size() + " unclosed threads in test: " + testClassName);
            for (Thread thread : threads) {
                StackTraceElement[] stackTrace = unclosedThreadMap.get(thread);
                log(getFullStacktrace(thread, stackTrace));
            }
            flushReportFile();
        }
    }

    private void log(String msg) {
        // logger.info(identifier + msg);
        String s = identifier + msg;
        System.out.println(s);
        if (reportWriter != null) {
            reportWriter.println(s);
        }
    }

    public static String getFullStacktrace(Thread thread, StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder("Thread: \"" + thread.getName() + "\"" + " Id="
            + thread.getId());
        sb.append(' ').append(thread.getState());
        sb.append('\n');
        if (stackTrace == null) {
            stackTrace = thread.getStackTrace();
        }
        for (StackTraceElement ste : stackTrace) {
            sb.append("    at ").append(ste.toString());
            sb.append('\n');
        }
        return sb.toString();
    }

}
