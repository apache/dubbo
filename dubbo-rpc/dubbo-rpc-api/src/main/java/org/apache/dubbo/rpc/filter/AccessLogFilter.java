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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.AccessLogData;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FILTER_VALIDATION_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.VULNERABILITY_WARNING;
import static org.apache.dubbo.rpc.Constants.ACCESS_LOG_FIXED_PATH_KEY;

/**
 * Record access log for the service.
 * <p>
 * Logger key is <code><b>dubbo.accesslog</b></code>.
 * In order to configure access log appear in the specified appender only, additivity need to be configured in log4j's
 * config file, for example:
 * <code>
 * <pre>
 * &lt;logger name="<b>dubbo.accesslog</b>" <font color="red">additivity="false"</font>&gt;
 *    &lt;level value="info" /&gt;
 *    &lt;appender-ref ref="foo" /&gt;
 * &lt;/logger&gt;
 * </pre></code>
 */
@Activate(group = PROVIDER)
public class AccessLogFilter implements Filter {

    public static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AccessLogFilter.class);

    private static final String LOG_KEY = "dubbo.accesslog";

    private static final int LOG_MAX_BUFFER = 5000;

    private static long LOG_OUTPUT_INTERVAL = 5000;

    private static final String FILE_DATE_FORMAT = "yyyyMMdd";

    // It's safe to declare it as singleton since it runs on single thread only
    private final DateFormat fileNameFormatter = new SimpleDateFormat(FILE_DATE_FORMAT);

    private final ConcurrentMap<String, Queue<AccessLogData>> logEntries = new ConcurrentHashMap<>();

    private final AtomicBoolean scheduled = new AtomicBoolean();
    private ScheduledFuture<?> future;

    private static final String LINE_SEPARATOR = "line.separator";

    /**
     * Default constructor initialize demon thread for writing into access log file with names with access log key
     * defined in url <b>accesslog</b>
     */
    public AccessLogFilter() {}

    /**
     * This method logs the access log for service method invocation call.
     *
     * @param invoker service
     * @param inv     Invocation service method.
     * @return Result from service method.
     * @throws RpcException
     */
    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        String accessLogKey = invoker.getUrl().getParameter(Constants.ACCESS_LOG_KEY);
        boolean isFixedPath = invoker.getUrl().getParameter(ACCESS_LOG_FIXED_PATH_KEY, true);
        if (StringUtils.isEmpty(accessLogKey) || "false".equalsIgnoreCase(accessLogKey)) {
            // Notice that disable accesslog of one service may cause the whole application to stop collecting
            // accesslog.
            // It's recommended to use application level configuration to enable or disable accesslog if dynamically
            // configuration is needed .
            if (future != null && !future.isCancelled()) {
                future.cancel(true);
                logger.info("Access log task cancelled ...");
            }
            return invoker.invoke(inv);
        }

        if (scheduled.compareAndSet(false, true)) {
            future = inv.getModuleModel()
                    .getApplicationModel()
                    .getFrameworkModel()
                    .getBeanFactory()
                    .getBean(FrameworkExecutorRepository.class)
                    .getSharedScheduledExecutor()
                    .scheduleWithFixedDelay(
                            new AccesslogRefreshTask(isFixedPath),
                            LOG_OUTPUT_INTERVAL,
                            LOG_OUTPUT_INTERVAL,
                            TimeUnit.MILLISECONDS);
            logger.info("Access log task started ...");
        }
        Optional<AccessLogData> optionalAccessLogData = Optional.empty();
        try {
            optionalAccessLogData = Optional.of(buildAccessLogData(invoker, inv));
        } catch (Throwable t) {
            logger.warn(
                    CONFIG_FILTER_VALIDATION_EXCEPTION,
                    "",
                    "",
                    "Exception in AccessLogFilter of service(" + invoker + " -> " + inv + ")",
                    t);
        }
        try {
            return invoker.invoke(inv);
        } finally {
            String finalAccessLogKey = accessLogKey;
            optionalAccessLogData.ifPresent(logData -> {
                logData.setOutTime(new Date());
                log(finalAccessLogKey, logData, isFixedPath);
            });
        }
    }

    private void log(String accessLog, AccessLogData accessLogData, boolean isFixedPath) {
        Queue<AccessLogData> logQueue =
                ConcurrentHashMapUtils.computeIfAbsent(logEntries, accessLog, k -> new ConcurrentLinkedQueue<>());

        if (logQueue.size() < LOG_MAX_BUFFER) {
            logQueue.add(accessLogData);
        } else {
            logger.warn(
                    CONFIG_FILTER_VALIDATION_EXCEPTION,
                    "",
                    "",
                    "AccessLog buffer is full. Do a force writing to file to clear buffer.");
            // just write current logSet to file.
            writeLogSetToFile(accessLog, logQueue, isFixedPath);
            // after force writing, add accessLogData to current logSet
            logQueue.add(accessLogData);
        }
    }

    private void writeLogSetToFile(String accessLog, Queue<AccessLogData> logSet, boolean isFixedPath) {
        try {
            if (ConfigUtils.isDefault(accessLog)) {
                processWithServiceLogger(logSet);
            } else {
                if (isFixedPath) {
                    logger.warn(
                            VULNERABILITY_WARNING,
                            "Change of accesslog file path not allowed. ",
                            "",
                            "Will write to the default location, \" +\n"
                                    + "                        \"please enable this feature by setting 'accesslog.fixed.path=true' and restart the process. \" +\n"
                                    + "                        \"We highly recommend to not enable this feature in production for security concerns, \" +\n"
                                    + "                        \"please be fully aware of the potential risks before doing so!");
                    processWithServiceLogger(logSet);
                } else {
                    logger.warn(
                            VULNERABILITY_WARNING,
                            "Accesslog file path changed to " + accessLog + ", be aware of possible vulnerabilities!",
                            "",
                            "");
                    File file = new File(accessLog);
                    createIfLogDirAbsent(file);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Append log to " + accessLog);
                    }
                    renameFile(file);
                    processWithAccessKeyLogger(logSet, file);
                }
            }
        } catch (Exception e) {
            logger.error(CONFIG_FILTER_VALIDATION_EXCEPTION, "", "", e.getMessage(), e);
        }
    }

    private void processWithAccessKeyLogger(Queue<AccessLogData> logQueue, File file) throws IOException {
        FileWriter writer = new FileWriter(file, true);
        try {
            while (!logQueue.isEmpty()) {
                writer.write(logQueue.poll().getLogMessage());
                writer.write(System.getProperty(LINE_SEPARATOR));
            }
        } finally {
            writer.flush();
            writer.close();
        }
    }

    private AccessLogData buildAccessLogData(Invoker<?> invoker, Invocation inv) {
        AccessLogData logData = AccessLogData.newLogData();
        logData.setServiceName(invoker.getInterface().getName());
        logData.setMethodName(RpcUtils.getMethodName(inv));
        logData.setVersion(invoker.getUrl().getVersion());
        logData.setGroup(invoker.getUrl().getGroup());
        logData.setInvocationTime(new Date());
        logData.setTypes(inv.getParameterTypes());
        logData.setArguments(inv.getArguments());
        return logData;
    }

    private void processWithServiceLogger(Queue<AccessLogData> logQueue) {
        while (!logQueue.isEmpty()) {
            AccessLogData logData = logQueue.poll();
            LoggerFactory.getLogger(LOG_KEY + "." + logData.getServiceName()).info(logData.getLogMessage());
        }
    }

    private void createIfLogDirAbsent(File file) {
        File dir = file.getParentFile();
        if (null != dir && !dir.exists()) {
            dir.mkdirs();
        }
    }

    private void renameFile(File file) {
        if (file.exists()) {
            String now = fileNameFormatter.format(new Date());
            String last = fileNameFormatter.format(new Date(file.lastModified()));
            if (!now.equals(last)) {
                File archive = new File(file.getAbsolutePath() + "." + now);
                file.renameTo(archive);
            }
        }
    }

    class AccesslogRefreshTask implements Runnable {
        private final boolean isFixedPath;

        public AccesslogRefreshTask(boolean isFixedPath) {
            this.isFixedPath = isFixedPath;
        }

        @Override
        public void run() {
            if (!AccessLogFilter.this.logEntries.isEmpty()) {
                for (Map.Entry<String, Queue<AccessLogData>> entry : AccessLogFilter.this.logEntries.entrySet()) {
                    String accessLog = entry.getKey();
                    Queue<AccessLogData> logSet = entry.getValue();
                    writeLogSetToFile(accessLog, logSet, isFixedPath);
                }
            }
        }
    }

    // test purpose only
    public static void setInterval(long interval) {
        LOG_OUTPUT_INTERVAL = interval;
    }

    // test purpose only
    public static long getInterval() {
        return LOG_OUTPUT_INTERVAL;
    }

    // test purpose only
    public void destroy() {
        future.cancel(true);
    }
}
