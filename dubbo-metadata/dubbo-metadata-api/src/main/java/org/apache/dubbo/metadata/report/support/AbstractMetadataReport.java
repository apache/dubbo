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
package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.FILE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.utils.StringUtils.replace;
import static org.apache.dubbo.metadata.report.support.Constants.CYCLE_REPORT_KEY;
import static org.apache.dubbo.metadata.report.support.Constants.DEFAULT_METADATA_REPORT_CYCLE_REPORT;
import static org.apache.dubbo.metadata.report.support.Constants.DEFAULT_METADATA_REPORT_RETRY_PERIOD;
import static org.apache.dubbo.metadata.report.support.Constants.DEFAULT_METADATA_REPORT_RETRY_TIMES;
import static org.apache.dubbo.metadata.report.support.Constants.RETRY_PERIOD_KEY;
import static org.apache.dubbo.metadata.report.support.Constants.RETRY_TIMES_KEY;
import static org.apache.dubbo.metadata.report.support.Constants.SYNC_REPORT_KEY;

/**
 *
 */
public abstract class AbstractMetadataReport implements MetadataReport {

    protected final static String DEFAULT_ROOT = "dubbo";

    private static final int ONE_DAY_IN_MILLISECONDS = 60 * 24 * 60 * 1000;
    private static final int FOUR_HOURS_IN_MILLISECONDS = 60 * 4 * 60 * 1000;
    // Log output
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    final Map<MetadataIdentifier, Object> allMetadataReports = new ConcurrentHashMap<>(4);

    final Map<MetadataIdentifier, Object> failedReports = new ConcurrentHashMap<>(4);

    private URL reportURL;
    boolean syncReport;

    // Local disk cache file
    File localCacheFile;
    // Local disk cache, where the special key value.registries records the list of metadata centers, and the others are the list of notified service providers
    final Properties properties = new Properties();

    private final AtomicLong lastCacheChanged = new AtomicLong();

    // ThreadPoolExecutors
    private final ExecutorService reportCacheExecutor;

    public final MetadataReportRetry metadataReportRetry;

    private final ScheduledExecutorService cycleReportExecutor;

    public AbstractMetadataReport(URL reportServerURL) {
        setUrl(reportServerURL);

        this.localCacheFile = initializeLocalCacheFile(reportServerURL);
        loadProperties();
        syncReport = reportServerURL.getParameter(SYNC_REPORT_KEY, false);
        metadataReportRetry = new MetadataReportRetry(reportServerURL.getParameter(RETRY_TIMES_KEY, DEFAULT_METADATA_REPORT_RETRY_TIMES),
                reportServerURL.getParameter(RETRY_PERIOD_KEY, DEFAULT_METADATA_REPORT_RETRY_PERIOD));
        this.reportCacheExecutor = newSingleThreadExecutor(new NamedThreadFactory("DubboSaveMetadataReport", true));
        this.cycleReportExecutor = newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboMetadataReportTimer", true));
        // cycle report the data switch
        if (reportServerURL.getParameter(CYCLE_REPORT_KEY, DEFAULT_METADATA_REPORT_CYCLE_REPORT)) {
            cycleReportExecutor.scheduleAtFixedRate(this::publishAll, calculateStartTime(), ONE_DAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        }
    }

    private File initializeLocalCacheFile(URL reportServerURL) {
        // Start file save timer
        String defaultFilename = System.getProperty("user.home") +
                "/.dubbo/dubbo-metadata-" +
                reportServerURL.getParameter(APPLICATION_KEY) + "-" +
                replace(reportServerURL.getAddress(), ":", "-") +
                ".cache";
        String filename = reportServerURL.getParameter(FILE_KEY, defaultFilename);
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid service store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
            // if this file exist, firstly delete it.
            if (!initialized.getAndSet(true) && file.exists()) {
                file.delete();
            }
        }
        return file;
    }

    public URL getUrl() {
        return reportURL;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("metadataReport url == null");
        }
        this.reportURL = url;
    }

    /**
     * 本地文件记录
     * @param version
     */
    private void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (localCacheFile == null) {
            return;
        }
        // Save
        try {
            File lockfile = new File(localCacheFile.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            try (RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
                 FileChannel channel = raf.getChannel()) {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    throw new IOException("Can not lock the metadataReport cache file " + localCacheFile.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.metadata.file=xxx.properties");
                }
                // Save
                try {
                    if (!localCacheFile.exists()) {
                        localCacheFile.createNewFile();
                    }
                    try (FileOutputStream outputFile = new FileOutputStream(localCacheFile)) {
                        properties.store(outputFile, "Dubbo metadataReport Cache");
                    }
                } finally {
                    lock.release();
                }
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

    void loadProperties() {
        if (localCacheFile != null && localCacheFile.exists()) {
            try (InputStream in = new FileInputStream(localCacheFile)) {
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    logger.info("Load service store file " + localCacheFile + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load service store file " + localCacheFile, e);
            }
        }
    }

    /**
     * 本地文件保存属性
     * @param metadataIdentifier
     * @param value
     * @param add
     * @param sync
     */
    private void saveProperties(MetadataIdentifier metadataIdentifier, String value, boolean add, boolean sync) {
        if (localCacheFile == null) {
            return;
        }

        try {
            /**
             * 向properties添加或删除属性
             */
            if (add) {
                properties.setProperty(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), value);
            } else {
                properties.remove(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
            }
            long version = lastCacheChanged.incrementAndGet();
            /**
             * 同步或异步
             */
            if (sync) {
                new SaveProperties(version).run();
            } else {
                reportCacheExecutor.execute(new SaveProperties(version));
            }

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
            /**
             * 本地文件保存
             */
            doSaveProperties(version);
        }
    }

    /**
     * 存储元数据
     * @param providerMetadataIdentifier
     * @param serviceDefinition
     */
    @Override
    public void storeProviderMetadata(MetadataIdentifier providerMetadataIdentifier, ServiceDefinition serviceDefinition) {
        if (syncReport) {
            //同步
            storeProviderMetadataTask(providerMetadataIdentifier, serviceDefinition);
        } else {
            //异步
            reportCacheExecutor.execute(() -> storeProviderMetadataTask(providerMetadataIdentifier, serviceDefinition));
        }
    }

    /**
     * 注册元数据
     * @param providerMetadataIdentifier
     * @param serviceDefinition
     */
    private void storeProviderMetadataTask(MetadataIdentifier providerMetadataIdentifier, ServiceDefinition serviceDefinition) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("store provider metadata. Identifier : " + providerMetadataIdentifier + "; definition: " + serviceDefinition);
            }
            allMetadataReports.put(providerMetadataIdentifier, serviceDefinition);
            failedReports.remove(providerMetadataIdentifier);
            Gson gson = new Gson();
            String data = gson.toJson(serviceDefinition);
            /**
             * 注册元数据  ConfigCenterBasedMetadataReport
             */
            doStoreProviderMetadata(providerMetadataIdentifier, data);
            /**
             * 本地文件保存属性
             */
            saveProperties(providerMetadataIdentifier, data, true, !syncReport);
        } catch (Exception e) {
            // retry again. If failed again, throw exception.
            failedReports.put(providerMetadataIdentifier, serviceDefinition);
            /**
             * 重试
             */
            metadataReportRetry.startRetryTask();
            logger.error("Failed to put provider metadata " + providerMetadataIdentifier + " in  " + serviceDefinition + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 存储服务消费者配置
     * @param consumerMetadataIdentifier
     * @param serviceParameterMap
     */
    @Override
    public void storeConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap) {
        if (syncReport) {
            storeConsumerMetadataTask(consumerMetadataIdentifier, serviceParameterMap);
        } else {
            reportCacheExecutor.execute(() -> storeConsumerMetadataTask(consumerMetadataIdentifier, serviceParameterMap));
        }
    }

    /**
     * 存储服务消费者配置
     * @param consumerMetadataIdentifier
     * @param serviceParameterMap
     */
    public void storeConsumerMetadataTask(MetadataIdentifier consumerMetadataIdentifier, Map<String, String> serviceParameterMap) {
        try {
            if (logger.isInfoEnabled()) {
                logger.info("store consumer metadata. Identifier : " + consumerMetadataIdentifier + "; definition: " + serviceParameterMap);
            }
            allMetadataReports.put(consumerMetadataIdentifier, serviceParameterMap);
            failedReports.remove(consumerMetadataIdentifier);

            Gson gson = new Gson();
            String data = gson.toJson(serviceParameterMap);
            /**
             * 存储元数据  ConfigCenterBasedMetadataReport
             */
            doStoreConsumerMetadata(consumerMetadataIdentifier, data);
            /**
             * 本地文件保存属性
             */
            saveProperties(consumerMetadataIdentifier, data, true, !syncReport);
        } catch (Exception e) {
            // retry again. If failed again, throw exception.
            failedReports.put(consumerMetadataIdentifier, serviceParameterMap);
            metadataReportRetry.startRetryTask();
            logger.error("Failed to put consumer metadata " + consumerMetadataIdentifier + ";  " + serviceParameterMap + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveServiceMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url) {
        if (syncReport) {
            doSaveMetadata(metadataIdentifier, url);
        } else {
            reportCacheExecutor.execute(() -> doSaveMetadata(metadataIdentifier, url));
        }
    }

    @Override
    public void removeServiceMetadata(ServiceMetadataIdentifier metadataIdentifier) {
        if (syncReport) {
            doRemoveMetadata(metadataIdentifier);
        } else {
            reportCacheExecutor.execute(() -> doRemoveMetadata(metadataIdentifier));
        }
    }

    @Override
    public List<String> getExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        // TODO, fallback to local cache
        return doGetExportedURLs(metadataIdentifier);
    }

    @Override
    public void saveSubscribedData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, Collection<String> urls) {
        if (syncReport) {
            doSaveSubscriberData(subscriberMetadataIdentifier, new Gson().toJson(urls));
        } else {
            reportCacheExecutor.execute(() -> doSaveSubscriberData(subscriberMetadataIdentifier, new Gson().toJson(urls)));
        }
    }


    @Override
    public Set<String> getSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        String content = doGetSubscribedURLs(subscriberMetadataIdentifier);
        Type setType = new TypeToken<SortedSet<String>>() {
        }.getType();
        return new Gson().fromJson(content, setType);
    }

    String getProtocol(URL url) {
        String protocol = url.getParameter(SIDE_KEY);
        protocol = protocol == null ? url.getProtocol() : protocol;
        return protocol;
    }

    /**
     * @return if need to continue
     */
    public boolean retry() {
        //重试
        return doHandleMetadataCollection(failedReports);
    }

    /**
     * 重新注册元数据信息
     * @param metadataMap
     * @return
     */
    private boolean doHandleMetadataCollection(Map<MetadataIdentifier, Object> metadataMap) {
        if (metadataMap.isEmpty()) {
            return true;
        }
        /**
         * 遍历metadataMap   依照side来重新注册元数据
         */
        Iterator<Map.Entry<MetadataIdentifier, Object>> iterable = metadataMap.entrySet().iterator();
        while (iterable.hasNext()) {
            Map.Entry<MetadataIdentifier, Object> item = iterable.next();
            if (PROVIDER_SIDE.equals(item.getKey().getSide())) {
                this.storeProviderMetadata(item.getKey(), (FullServiceDefinition) item.getValue());
            } else if (CONSUMER_SIDE.equals(item.getKey().getSide())) {
                this.storeConsumerMetadata(item.getKey(), (Map) item.getValue());
            }

        }
        return false;
    }

    /**
     * not private. just for unittest.
     */
    void publishAll() {
        logger.info("start to publish all metadata.");
        this.doHandleMetadataCollection(allMetadataReports);
    }

    /**
     * between 2:00 am to 6:00 am, the time is random.
     *
     * @return
     */
    long calculateStartTime() {
        Calendar calendar = Calendar.getInstance();
        long nowMill = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long subtract = calendar.getTimeInMillis() + ONE_DAY_IN_MILLISECONDS - nowMill;
        return subtract + (FOUR_HOURS_IN_MILLISECONDS / 2) + ThreadLocalRandom.current().nextInt(FOUR_HOURS_IN_MILLISECONDS);
    }

    class MetadataReportRetry {
        protected final Logger logger = LoggerFactory.getLogger(getClass());

        final ScheduledExecutorService retryExecutor = newScheduledThreadPool(0, new NamedThreadFactory("DubboMetadataReportRetryTimer", true));
        volatile ScheduledFuture retryScheduledFuture;
        final AtomicInteger retryCounter = new AtomicInteger(0);
        // retry task schedule period
        long retryPeriod;
        // if no failed report, wait how many times to run retry task.
        int retryTimesIfNonFail = 600;

        int retryLimit;

        public MetadataReportRetry(int retryTimes, int retryPeriod) {
            this.retryPeriod = retryPeriod;
            this.retryLimit = retryTimes;
        }

        void startRetryTask() {
            if (retryScheduledFuture == null) {
                synchronized (retryCounter) {
                    if (retryScheduledFuture == null) {
                        retryScheduledFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
                            @Override
                            public void run() {
                                // Check and connect to the metadata
                                try {
                                    //记录重试次数
                                    int times = retryCounter.incrementAndGet();
                                    logger.info("start to retry task for metadata report. retry times:" + times);
                                    //重试且计算次数
                                    if (retry() && times > retryTimesIfNonFail) {
                                        cancelRetryTask();
                                    }
                                    if (times > retryLimit) {
                                        cancelRetryTask();
                                    }
                                } catch (Throwable t) { // Defensive fault tolerance
                                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                                }
                            }
                        }, 500, retryPeriod, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }

        void cancelRetryTask() {
            if (retryScheduledFuture != null) {
                retryScheduledFuture.cancel(false);
            }
            shutdown(retryExecutor);
        }
    }

    private void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, List<String> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return;
        }
        List<String> encodedUrlList = new ArrayList<>(urls.size());
        for (String url : urls) {
            encodedUrlList.add(URL.encode(url));
        }
        doSaveSubscriberData(subscriberMetadataIdentifier, encodedUrlList);
    }

    @Override
    public final void close() throws Exception {
        this.shutdownThreadPoolExecutors();
        this.clearCache();
        doClose();
    }

    protected abstract void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions);

    protected abstract void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString);

    protected abstract void doSaveMetadata(ServiceMetadataIdentifier metadataIdentifier, URL url);

    protected abstract void doRemoveMetadata(ServiceMetadataIdentifier metadataIdentifier);

    protected abstract List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier);

    protected abstract void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr);

    protected abstract String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier);

    /**
     * Close other resources
     *
     * @since 2.7.8
     */
    protected void doClose() throws Exception {

    }

    private void clearCache() {
        this.properties.clear();
        this.allMetadataReports.clear();
        this.failedReports.clear();
        this.localCacheFile.delete();
    }

    private void shutdownThreadPoolExecutors() {
        this.metadataReportRetry.cancelRetryTask();
        shutdown(this.reportCacheExecutor);
        shutdown(cycleReportExecutor);
    }

    private static void shutdown(ExecutorService executorService) {
        if (executorService == null) {
            return;
        }
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

}
