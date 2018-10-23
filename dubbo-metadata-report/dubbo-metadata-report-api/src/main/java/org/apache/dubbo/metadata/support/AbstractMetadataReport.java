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
package org.apache.dubbo.metadata.support;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.metadata.store.MetadataReport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 */
public abstract class AbstractMetadataReport implements MetadataReport {


    private final static String TAG = "sd.";
    // Log output
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    // Local disk cache, where the special key value.registies records the list of registry centers, and the others are the list of notified service providers
    final Properties properties = new Properties();
    // File cache timing writing
    private final ExecutorService reportCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveServicestoreCache", true));

    private final AtomicLong lastCacheChanged = new AtomicLong();
    final Set<URL> failedReports = new ConcurrentHashSet<URL>(4);
    private URL reportURL;
    // Local disk cache file
    File file;
    private AtomicBoolean INIT = new AtomicBoolean(false);
    final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(0, new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));
    ScheduledFuture retryScheduledFuture;
    AtomicInteger retryTimes = new AtomicInteger(0);
    // retry task schedule period
    long retryPeriod = 3000L ;
    // if no failed report, wait how many times to run retry task.
    int retryTimesIfNonFail = 600;

    public AbstractMetadataReport(URL reportURL) {
        setUrl(reportURL);
        // Start file save timer
        String filename = reportURL.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-servicestore-" + reportURL.getParameter(Constants.APPLICATION_KEY) + "-" + reportURL.getAddress() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid service store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
            // if this file exist, firstly delete it.
            if (!INIT.getAndSet(true) && file.exists()) {
                file.delete();
            }
        }
        this.file = file;
        loadProperties();
    }

    public URL getUrl() {
        return reportURL;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("servicestore url == null");
        }
        this.reportURL = url;
    }

    private void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        // Save
        try {
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    FileLock lock = channel.tryLock();
                    if (lock == null) {
                        throw new IOException("Can not lock the servicestore cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.servicestore.file=xxx.properties");
                    }
                    // Save
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream outputFile = new FileOutputStream(file);
                        try {
                            properties.store(outputFile, "Dubbo Servicestore Cache");
                        } finally {
                            outputFile.close();
                        }
                    } finally {
                        lock.release();
                    }
                } finally {
                    channel.close();
                }
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            if (version < lastCacheChanged.get()) {
                return;
            } else {
                reportCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save service store file, cause: " + e.getMessage(), e);
        }
    }

    void startRetryTask(){
        if(retryScheduledFuture == null) {
            synchronized (failedReports){
                if(retryScheduledFuture == null){
                    retryScheduledFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
                        @Override
                        public void run() {
                            // Check and connect to the registry
                            try {
                                retry();
                            } catch (Throwable t) { // Defensive fault tolerance
                                logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                            }
                        }
                    }, 500, retryPeriod, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    void cancelRetryTask(){
        retryScheduledFuture.cancel(false);
        retryExecutor.shutdown();
    }

    void loadProperties() {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    logger.info("Load service store file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load service store file " + file, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void saveProperties(URL url, boolean add) {
        if (file == null) {
            return;
        }

        try {
            if (add) {
                properties.setProperty(url.getServiceKey(), url.toFullString());
            } else {
                properties.remove(url.getServiceKey());
            }
            long version = lastCacheChanged.incrementAndGet();
            reportCacheExecutor.execute(new SaveProperties(version));
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    @Override
    public String toString() {
        return getUrl().toString();
    }

    private class SaveProperties implements Runnable {
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        @Override
        public void run() {
            doSaveProperties(version);
        }
    }

    public void put(URL url) {
        try {
            // remove the individul param
            url = url.removeParameters(Constants.PID_KEY, Constants.TIMESTAMP_KEY);
            if (logger.isInfoEnabled()) {
                logger.info("Servicestore Put: " + url);
            }
            failedReports.remove(url);
            doPut(url);
            saveProperties(url, true);
        } catch (Exception e) {
            // retry again. If failed again, throw exception.
            failedReports.add(url);
            startRetryTask();
            logger.error("Failed to put servicestore " + url + " in  " + getUrl().toFullString() + ", cause: " + e.getMessage(), e);
        }
    }


    public URL peek(URL url) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Servicestore Peek: " + url);
            }
            return doPeek(url);
        } catch (Exception e) {
            logger.error("Failed to peek servicestore " + url + " in  " + getUrl().toFullString() + ", cause: " + e.getMessage(), e);
        }
        return null;
    }

    public String getUrlKey(URL url) {
        String protocol = getProtocol(url);
        String app = url.getParameter(Constants.APPLICATION_KEY);
        String appStr = Constants.PROVIDER_PROTOCOL.equals(protocol) ? "" : (app == null ? "" : (app + "."));
        return TAG + protocol + "." + appStr + url.getServiceKey();
    }

    String getProtocol(URL url) {
        String protocol = url.getParameter(Constants.SIDE_KEY);
        protocol = protocol == null ? url.getProtocol() : protocol;
        return protocol;
    }

    public void retry() {
        if (retryTimes.incrementAndGet() > retryTimesIfNonFail && failedReports.isEmpty()) {
            this.cancelRetryTask();
        }
        if (failedReports.isEmpty()) {
            return;
        }
        for (URL url : new HashSet<URL>(failedReports)) {
            this.put(url);
        }
    }


    protected abstract void doPut(URL url);

    protected abstract URL doPeek(URL url);

}
