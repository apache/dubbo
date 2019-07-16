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
package org.apache.dubbo.common.config.configcenter.file;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeEvent;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.function.ThrowableConsumer;
import org.apache.dubbo.common.utils.NamedThreadFactory;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

/**
 * File-System based {@link DynamicConfiguration} implementation
 *
 * @since 2.7.4
 */
public class FileSystemDynamicConfiguration implements DynamicConfiguration {

    public static final String CONFIG_CENTER_DIR_PARAM_NAME = "dubbo.config-center.dir";

    public static final String CONFIG_CENTER_THREAD_POOL_NAME_PARAM_NAME = "dubbo.config-center.thread-pool.name";

    public static final String CONFIG_CENTER_ENCODING_PARAM_NAME = "dubbo.config-center.encoding";

    public static final String DEFAULT_CONFIG_CENTER_DIR_PATH = System.getProperty("user.home") + File.separator
            + ".dubbo" + File.separator + "config-center";

    public static final String DEFAULT_CONFIG_CENTER_THREAD_POOL_NAME = "dubbo-config-center-workers";


    public static final String DEFAULT_CONFIG_CENTER_ENCODING = "UTF-8";

    private static final WatchEvent.Kind[] INTEREST_PATH_KINDS = of(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

    /**
     * The class name of {@linkplain sun.nio.fs.PollingWatchService}
     */
    private static final String POLLING_WATCH_SERVICE_CLASS_NAME = "sun.nio.fs.PollingWatchService";

    private static final int WORKERS_THREAD_POOL_SIZE = 1;

    /**
     * Logger
     */
    private static final Log logger = LogFactory.getLog(FileSystemDynamicConfiguration.class);

    /**
     * The unmodifiable map for {@link ConfigChangeType} whose key is the {@link WatchEvent.Kind#name() name} of
     * {@link WatchEvent.Kind WatchEvent's Kind}
     */
    private static final Map<String, ConfigChangeType> CONFIG_CHANGE_TYPES_MAP =
            unmodifiableMap(new HashMap<String, ConfigChangeType>() {
                // Initializes the elements that is mapping ConfigChangeType
                {
                    put(ENTRY_CREATE.name(), ConfigChangeType.ADDED);
                    put(ENTRY_DELETE.name(), ConfigChangeType.DELETED);
                    put(ENTRY_MODIFY.name(), ConfigChangeType.MODIFIED);
                }
            });


    /**
     * The Root Directory for config center
     */
    private final File directory;

    /**
     * The thread pool for config's tasks
     *
     * @see ScheduledThreadPoolExecutor
     */
    private final ScheduledThreadPoolExecutor workersThreadPool;

    /**
     * The thread pool for {@link WatchEvent WatchEvents} loop
     * It's optional if there is not any {@link ConfigurationListener} registration
     *
     * @see ExecutorService
     */
    private ExecutorService watchEventsLoopThreadPool;

    private final String encoding;

    private final Optional<WatchService> watchService;

    private final Map<Path, List<ConfigurationListener>> listenersRepository;

    private final Lock lock = new ReentrantLock();

    /**
     * Is Pooling Based Watch Service
     *
     * @see #isPoolingBasedWatchService(Optional)
     */
    private final boolean basedPoolingWatchService;

    private final WatchEvent.Modifier[] modifiers;

    /**
     * the delay to {@link #publishConfig(String, String, String) publish} in seconds.
     * If null, execute indirectly
     */
    private final Integer delayToPublish;

    public FileSystemDynamicConfiguration(URL url) {
        this.directory = initDirectory(url);
        this.workersThreadPool = newWorkersThreadPool(url);
        this.encoding = getEncoding(url);
        this.watchService = newWatchService();
        this.listenersRepository = new LinkedHashMap<>();
        this.basedPoolingWatchService = isPoolingBasedWatchService(watchService);
        this.modifiers = initWatchEventModifiers(watchService);
        this.delayToPublish = initDelayToPublish(modifiers);
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        doInListener(key, group, (configFilePath, listeners) -> {

            if (listeners.isEmpty()) { // If no element, it indicates watchService was registered before
                ThrowableConsumer.execute(configFilePath, path -> {
                    File configFile = path.toFile();
                    FileUtils.forceMkdirParent(configFile);
                    // A directory to be watched
                    File watchedDirectory = configFile.getParentFile();
                    if (watchedDirectory != null) {
                        watchedDirectory.toPath().register(watchService.get(), INTEREST_PATH_KINDS, modifiers);
                    }
                });
            }

            // Add into cache
            listeners.add(listener);
        });
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        doInListener(key, group, (file, listeners) -> {
            // Remove into cache
            listeners.remove(listener);
        });
    }

    protected File groupDirectory(String group) {
        String actualGroup = isBlank(group) ? DEFAULT_GROUP : group;
        return new File(directory, actualGroup);
    }

    protected File configFile(String key, String group) {
        return new File(groupDirectory(group), key);
    }


    private void doInListener(String key, String group, BiConsumer<Path, List<ConfigurationListener>> consumer) {
        watchService.ifPresent(watchService -> {
            executeMutually(() -> {
                // process the WatchEvents if there is not any listener
                if (!initWatchEventsLoopThreadPool()) {
                    processWatchEventsLoop(watchService);
                }

                File configFile = configFile(key, group);
                Path configFilePath = configFile.toPath();
                List<ConfigurationListener> listeners = getListeners(configFilePath);
                consumer.accept(configFilePath, listeners);

                // Nothing to return
                return void.class;
            });
        });
    }

    /**
     * Initializes {@link #watchEventsLoopThreadPool} and return initialized or not
     *
     * @return if <code>watchEventsLoopThreadPool</code> is not <code>null</code>, return null
     */
    private boolean initWatchEventsLoopThreadPool() {
        boolean initialized = this.watchEventsLoopThreadPool != null;
        if (!initialized) {
            this.watchEventsLoopThreadPool = newWatchEventsLoopThreadPool();
        }
        return initialized;
    }

    /**
     * Process the {@link WatchEvent WatchEvents} loop in async execution
     *
     * @param watchService {@link WatchService}
     */
    private void processWatchEventsLoop(WatchService watchService) {

        getWatchEventsLoopThreadPool().execute(() -> { // WatchEvents Loop
            while (true) {
                WatchKey watchKey = null;
                try {
                    watchKey = watchService.take();
                    if (watchKey.isValid()) {
                        for (WatchEvent event : watchKey.pollEvents()) {
                            WatchEvent.Kind kind = event.kind();
                            // configChangeType's key to match WatchEvent's Kind
                            ConfigChangeType configChangeType = CONFIG_CHANGE_TYPES_MAP.get(kind.name());
                            if (configChangeType != null) {
                                Path parentPath = (Path) watchKey.watchable();
                                Path currentPath = (Path) event.context();
                                Path actualPath = parentPath.resolve(currentPath);
                                fireConfigChangeEvent(actualPath, configChangeType);
                            }
                        }
                    }
                } catch (Exception e) {
                    return;
                } finally {
                    if (watchKey != null) {
                        // reset
                        watchKey.reset();
                    }
                }
            }
        });
    }

    private List<ConfigurationListener> getListeners(Path configFilePath) {
        return executeMutually(() -> listenersRepository.computeIfAbsent(configFilePath, p -> new LinkedList<>()));
    }

    private void fireConfigChangeEvent(Path configFilePath, ConfigChangeType configChangeType) {
        File watchedFile = configFilePath.toFile();
        String key = watchedFile.getName();
        String value = getConfig(watchedFile, -1L);
        // fire ConfigChangeEvent one by one
        getListeners(configFilePath).forEach(listener -> {
            listener.process(new ConfigChangeEvent(key, value, configChangeType));
        });
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        File configFile = configFile(key, group);
        return getConfig(configFile, timeout);
    }

    protected String getConfig(File configFile, long timeout) {
        return canRead(configFile) ? execute(() -> readFileToString(configFile, getEncoding()), timeout) : null;
    }

    private boolean canRead(File file) {
        return file.exists() && file.canRead();
    }

    @Override
    public String getConfigs(String key, String group, long timeout) throws IllegalStateException {
        return getConfig(key, group, timeout);
    }

    @Override
    public Object getInternalProperty(String key) {
        return null;
    }

    @Override
    public boolean publishConfig(String key, String group, String content) {

        File configFile = configFile(key, group);

        boolean published = false;

        try {
            published = publish(() -> {
                FileUtils.write(configFile, content, getEncoding());
                return true;
            });
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }

        return published;
    }

    private boolean publish(Callable<Boolean> callable) throws Exception {

        Integer delay = getDelayToPublish();

        if (delay != null) {
            ScheduledFuture<Boolean> future = getWorkersThreadPool().schedule(callable, delay.longValue(), SECONDS);
            return future.get();
        } else {
            return callable.call();
        }
    }

    @Override
    public Set<String> getConfigKeys(String group) throws UnsupportedOperationException {
        return Stream.of(groupDirectory(group).listFiles(File::isFile))
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, String> getConfigs(String group) throws UnsupportedOperationException {
        return getConfigs(group, -1);
    }

    private <V> V execute(Callable<V> task, long timeout) {
        V value = null;
        try {

            if (timeout < 1) { // less or equal 0
                value = task.call();
            } else {
                Future<V> future = workersThreadPool.submit(task);
                value = future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return value;
    }

    protected File getDirectory() {
        return directory;
    }

    protected ScheduledThreadPoolExecutor getWorkersThreadPool() {
        return workersThreadPool;
    }

    protected String getEncoding() {
        return encoding;
    }

    protected Integer getDelayToPublish() {
        return delayToPublish;
    }

    protected boolean isBasedPoolingWatchService() {
        return basedPoolingWatchService;
    }

    public ExecutorService getWatchEventsLoopThreadPool() {
        return watchEventsLoopThreadPool;
    }

    private <V> V executeMutually(Callable<V> callable) {
        V value = null;
        lock.lock();
        try {
            value = callable.call();
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        } finally {
            lock.unlock();
        }

        return value;
    }

    private static <T> T[] of(T... values) {
        return values;
    }


    private static Integer initDelayToPublish(WatchEvent.Modifier[] modifiers) {
        return Stream.of(modifiers)
                .filter(modifier -> modifier instanceof SensitivityWatchEventModifier)
                .map(SensitivityWatchEventModifier.class::cast)
                .map(SensitivityWatchEventModifier::sensitivityValueInSeconds)
                .max(Integer::compareTo)
                .orElse(null);
    }

    /**
     * Create a new {@link ScheduledThreadPoolExecutor} whose core and max pool size both are {@link #WORKERS_THREAD_POOL_SIZE 1}
     *
     * @param url
     * @return
     */
    private static ScheduledThreadPoolExecutor newWorkersThreadPool(URL url) {
        String threadPoolName = url.getParameter(CONFIG_CENTER_THREAD_POOL_NAME_PARAM_NAME, DEFAULT_CONFIG_CENTER_THREAD_POOL_NAME);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(WORKERS_THREAD_POOL_SIZE, new NamedThreadFactory(threadPoolName));
        executor.setMaximumPoolSize(WORKERS_THREAD_POOL_SIZE);
        return executor;
    }

    private static WatchEvent.Modifier[] initWatchEventModifiers(Optional<WatchService> watchService) {
        if (isPoolingBasedWatchService(watchService)) { // If based on PollingWatchService, High sensitivity will be used
            return of(SensitivityWatchEventModifier.HIGH);
        } else {
            return of();
        }
    }

    /**
     * It's whether the argument of {@link WatchService} is based on {@linkplain sun.nio.fs.PollingWatchService}
     * or not.
     * <p>
     * Some platforms do not provide the native implementation of {@link WatchService}, just use
     * {@linkplain sun.nio.fs.PollingWatchService} in periodic poll file modifications.
     *
     * @param watchService the instance of {@link WatchService}
     * @return if based, return <code>true</code>, or <code>false</code>
     */
    private static boolean isPoolingBasedWatchService(Optional<WatchService> watchService) {
        String className = watchService.map(Object::getClass).map(Class::getName).orElse(null);
        return POLLING_WATCH_SERVICE_CLASS_NAME.equals(className);
    }

    private static Optional<WatchService> newWatchService() {
        Optional<WatchService> watchService = null;
        FileSystem fileSystem = FileSystems.getDefault();
        try {
            watchService = Optional.of(fileSystem.newWatchService());
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            watchService = Optional.empty();
        }
        return watchService;
    }

    private static File initDirectory(URL url) {
        String directoryPath = url.getParameter(CONFIG_CENTER_DIR_PARAM_NAME, DEFAULT_CONFIG_CENTER_DIR_PATH);
        File rootDirectory = new File(url.getParameter(CONFIG_CENTER_DIR_PARAM_NAME, DEFAULT_CONFIG_CENTER_DIR_PATH));
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw new IllegalStateException(format("Dubbo config center directory[%s] can't be created!",
                    directoryPath));
        }
        return rootDirectory;
    }

    private static String getEncoding(URL url) {
        return url.getParameter(CONFIG_CENTER_ENCODING_PARAM_NAME, DEFAULT_CONFIG_CENTER_ENCODING);
    }

    private static ExecutorService newWatchEventsLoopThreadPool() {
        return newSingleThreadExecutor(new NamedThreadFactory("dubbo-config-center-watch-events-loop"));
    }
}
