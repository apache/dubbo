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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.AccessLogData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.rpc.Constants.ACCESS_LOG_KEY;

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
@Activate(group = PROVIDER, value = ACCESS_LOG_KEY)
public class AccessLogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    private static final String LOG_KEY = "dubbo.accesslog";

    private static final String LINE_SEPARATOR = "line.separator";

    private static final int LOG_MAX_BUFFER = 5000;

    private static final long LOG_OUTPUT_INTERVAL = 5000;

    private static final String FILE_DATE_FORMAT = "yyyyMMdd";

    // It's safe to declare it as singleton since it runs on single thread only
    private static final DateFormat FILE_NAME_FORMATTER = new SimpleDateFormat(FILE_DATE_FORMAT);

    private static final Map<String, Queue<AccessLogData>> LOG_ENTRIES = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService LOG_SCHEDULED = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-Access-Log", true));

    /**
     * Default constructor initialize demon thread for writing into access log file with names with access log key
     * defined in url <b>accesslog</b>
     */
    public AccessLogFilter() {
        LOG_SCHEDULED.scheduleWithFixedDelay(this::writeLogToFile, LOG_OUTPUT_INTERVAL, LOG_OUTPUT_INTERVAL, TimeUnit.MILLISECONDS);
    }

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
        try {
            String accessLogKey = invoker.getUrl().getParameter(ACCESS_LOG_KEY);
            if (ConfigUtils.isNotEmpty(accessLogKey)) {
                AccessLogData logData = AccessLogData.newLogData(); 
                logData.buildAccessLogData(invoker, inv);
                log(accessLogKey, logData);
            }
        } catch (Throwable t) {
            logger.warn("Exception in AccessLogFilter of service(" + invoker + " -> " + inv + ")", t);
        }
        return invoker.invoke(inv);
    }

    private void log(String accessLog, AccessLogData accessLogData) {
        Queue<AccessLogData> logQueue = LOG_ENTRIES.computeIfAbsent(accessLog, k -> new ConcurrentLinkedQueue<>());

        if (logQueue.size() < LOG_MAX_BUFFER) {
            logQueue.add(accessLogData);
        } else {
            logger.warn("AccessLog buffer is full. Do a force writing to file to clear buffer.");
            //just write current logQueue to file.
            writeLogQueueToFile(accessLog, logQueue);
            //after force writing, add accessLogData to current logQueue
            logQueue.add(accessLogData);
        }
    }

    private void writeLogQueueToFile(String accessLog, Queue<AccessLogData> logQueue) {
        try {
            if (ConfigUtils.isDefault(accessLog)) {
                processWithServiceLogger(logQueue);
            } else {
                File file = new File(accessLog);
                createIfLogDirAbsent(file);
                if (logger.isDebugEnabled()) {
                    logger.debug("Append log to " + accessLog);
                }
                renameFile(file);
                processWithAccessKeyLogger(logQueue, file);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void writeLogToFile() {
        if (!LOG_ENTRIES.isEmpty()) {
            for (Map.Entry<String, Queue<AccessLogData>> entry : LOG_ENTRIES.entrySet()) {
                String accessLog = entry.getKey();
                Queue<AccessLogData> logQueue = entry.getValue();
                writeLogQueueToFile(accessLog, logQueue);
            }
        }
    }

    private void processWithAccessKeyLogger(Queue<AccessLogData> logQueue, File file) throws IOException {
        FileWriter writer = new FileWriter(file, true);
        try  {
            while (!logQueue.isEmpty()) {
                writer.write(logQueue.poll().getLogMessage());
                writer.write(System.getProperty(LINE_SEPARATOR));
            }
        }finally {
            writer.flush();
            writer.close();
        }
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
            String now = FILE_NAME_FORMATTER.format(new Date());
            String last = FILE_NAME_FORMATTER.format(new Date(file.lastModified()));
            if (!now.equals(last)) {
                File archive = new File(file.getAbsolutePath() + "." + last);
                file.renameTo(archive);
            }
        }
    }
}
