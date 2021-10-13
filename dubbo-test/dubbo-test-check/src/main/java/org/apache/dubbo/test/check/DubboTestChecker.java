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

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A test listener to check unclosed threads of test.
 *
 * <pre>
 * Usages:
 *  mvn test -Dcheck_threads=true
 *  mvn test -Dcheck_threads=true -Dthread_dump_wait_time=5000
 * </pre>
 */
public class DubboTestChecker implements TestExecutionListener {

    private static final String DUBBO_TEST_CHECK_MODE = "check_mode";
    private static final String DUBBO_TEST_CHECK_THREADS = "check_threads";
    private static final String DUBBO_TEST_THREAD_DUMP_WAIT_TIME = "thread_dump_wait_time";
    private static final String DUBBO_TEST_FORCE_DESTROY = "force_destroy";
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
     * thread -> stacktrace
     */
    private Map<Thread, StackTraceElement[]> unclosedThreadMap = new ConcurrentHashMap<>();
    // test class name -> thread list
    private Map<String, List<Thread>> unclosedThreadsOfTestMap = new ConcurrentHashMap<>();
    private String identifier;


    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // log prefix
        identifier = "[" + this.getClass().getSimpleName() + "] ";

        // check_mode: class/method
        checkMode = StringUtils.lowerCase(System.getProperty(DUBBO_TEST_CHECK_MODE, MODE_CLASS));
        // check_threads: true/false
        checkThreads = Boolean.parseBoolean(System.getProperty(DUBBO_TEST_CHECK_THREADS, "false"));
        // thread_dump_wait_time
        threadDumpWaitTimeMs = Long.parseLong(System.getProperty(DUBBO_TEST_THREAD_DUMP_WAIT_TIME, "5000"));
        // force destroy dubbo
        forceDestroyDubboAfterClass = Boolean.parseBoolean(System.getProperty(DUBBO_TEST_FORCE_DESTROY, "true"));

        log(String.format("Dubbo test checker configs: checkMode=%s, checkThreads=%s, threadDumpWaitTimeMs=%s, forceDestroy=%s",
            checkMode, checkThreads, threadDumpWaitTimeMs, forceDestroyDubboAfterClass));
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {

        // print all unclosed threads
        if (checkThreads) {
            log("");
            log("Total found " + unclosedThreadMap.size() + " unclosed threads in " + unclosedThreadsOfTestMap.size() + " tests.");
            log("");
            unclosedThreadsOfTestMap.forEach((testClassName, threads) -> {
                printUnclosedThreads(threads, testClassName);
            });
        } else {
            log("Threads checking is disabled, use -Dcheck_threads=true to check unclosed threads.");
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
//        TestSource testSource = testIdentifier.getSource().orElse(null);
//        if (testSource instanceof ClassSource) {
//            ClassSource source = (ClassSource) testSource;
//            log("Run test class: " + source.getClassName());
//        } else if (testSource instanceof MethodSource) {
//            MethodSource source = (MethodSource) testSource;
//            log("Run test method: " + source.getClassName() + "#" + source.getMethodName());
//        }
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

    private void checkUnclosedThreads(String testClassName, long waitMs) {
        // wait for shutdown
        log("Wait " + waitMs + "ms to check threads ...");
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
        }

        Map<Thread, StackTraceElement[]> threadStacks = Thread.getAllStackTraces();
        List<Thread> unclosedThreads = threadStacks.keySet().stream()
            .filter(thread -> !StringUtils.startsWithAny(thread.getName(),
                "Reference Handler", "Finalizer", "Signal Dispatcher", "Attach Listener", "main" // jvm
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
    }

    private void printUnclosedThreads(List<Thread> threads, String testClassName) {
        if (threads.size() > 0) {
            log("Found " +threads.size()+  " unclosed threads in test: " + testClassName);
            for (Thread thread : threads) {
                StackTraceElement[] stackTrace = unclosedThreadMap.get(thread);
                log(getFullStacktrace(thread, stackTrace));
            }
        }
    }

    private void log(String msg) {
        // logger.info(identifier + msg);
        System.out.println(identifier + msg);
    }

    public static String getFullStacktrace(Thread thread, StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder("Thread: \"" + thread.getName() + "\"" + " Id="
            + thread.getId());
        sb.append(" ").append(thread.getState());
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
