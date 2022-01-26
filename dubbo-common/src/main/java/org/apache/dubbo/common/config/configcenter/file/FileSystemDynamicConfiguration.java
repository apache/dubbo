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
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration;
import org.apache.dubbo.common.function.ThrowableConsumer;
import org.apache.dubbo.common.function.ThrowableFunction;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * File-System based {@link DynamicConfiguration} implementation
 *
 * @since 2.7.5
 */
public class FileSystemDynamicConfiguration extends TreePathDynamicConfiguration {

    public static final String CONFIG_CENTER_DIR_PARAM_NAME = PARAM_NAME_PREFIX + "dir";

    public static final String CONFIG_CENTER_ENCODING_PARAM_NAME = PARAM_NAME_PREFIX + "encoding";

    public static final String DEFAULT_CONFIG_CENTER_DIR_PATH = System.getProperty("user.home") + File.separator
            + ".dubbo" + File.separator + "config-center";

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    public static final String DEFAULT_CONFIG_CENTER_ENCODING = "UTF-8";

    private static final WatchEvent.Kind[] INTEREST_PATH_KINDS = of(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

    /**
     * The class name of {@linkplain sun.nio.fs.PollingWatchService}
     */
    private static final String POLLING_WATCH_SERVICE_CLASS_NAME = "sun.nio.fs.PollingWatchService";

    private static final int THREAD_POOL_SIZE = 1;

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

    private static final Optional<WatchService> watchService;

    /**
     * Is Pooling Based Watch Service
     *
     * @see #detectPoolingBasedWatchService(Optional)
     */
    private static final boolean BASED_POOLING_WATCH_SERVICE;

    private static final WatchEvent.Modifier[] MODIFIERS;

    /**
     * the delay to action in seconds. If null, execute indirectly
     */
    private static final Integer DELAY;

    /**
     * The thread pool for {@link WatchEvent WatchEvents} loop
     * It's optional if there is not any {@link ConfigurationListener} registration
     *
     * @see ThreadPoolExecutor
     */
    private static final ThreadPoolExecutor WATCH_EVENTS_LOOP_THREAD_POOL;

    // static initialization
    static {
        watchService = newWatchService();
        BASED_POOLING_WATCH_SERVICE = detectPoolingBasedWatchService(watchService);
        MODIFIERS = initWatchEventModifiers();
        DELAY = initDelay(MODIFIERS);
        WATCH_EVENTS_LOOP_THREAD_POOL = newWatchEventsLoopThreadPool();
    }

    /**
     * The Root Directory for config center
     */
    private final File rootDirectory;

    private final String encoding;

    /**
     * The {@link Set} of {@link #groupDirectory(String) directories} that may be processing,
     * <p>
     * if {@link #isBasedPoolingWatchService()} is <code>false</code>, this properties will be
     * {@link Collections#emptySet() empty}
     *
     * @see #initProcessingDirectories()
     */
    private final Set<File> processingDirectories;

    private final Map<File, List<ConfigurationListener>> listenersRepository;
    private ScopeModel scopeModel;
    private AtomicBoolean hasRegisteredShutdownHook = new AtomicBoolean();

    public FileSystemDynamicConfiguration() {
        this(new File(DEFAULT_CONFIG_CENTER_DIR_PATH));
    }

    public FileSystemDynamicConfiguration(File rootDirectory) {
        this(rootDirectory, DEFAULT_CONFIG_CENTER_ENCODING);
    }

    public FileSystemDynamicConfiguration(File rootDirectory, String encoding) {
        this(rootDirectory, encoding, DEFAULT_THREAD_POOL_PREFIX);
    }

    public FileSystemDynamicConfiguration(File rootDirectory, String encoding, String threadPoolPrefixName) {
        this(rootDirectory, encoding, threadPoolPrefixName, DEFAULT_THREAD_POOL_SIZE);
    }

    public FileSystemDynamicConfiguration(File rootDirectory, String encoding, String threadPoolPrefixName,
                                          int threadPoolSize) {
        this(rootDirectory, encoding, threadPoolPrefixName, threadPoolSize, DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME);
    }

    public FileSystemDynamicConfiguration(File rootDirectory, String encoding,
                                          String threadPoolPrefixName,
                                          int threadPoolSize,
                                          long keepAliveTime) {
        super(rootDirectory.getAbsolutePath(), threadPoolPrefixName, threadPoolSize, keepAliveTime, DEFAULT_GROUP, -1L);
        this.rootDirectory = rootDirectory;
        this.encoding = encoding;
        this.processingDirectories = initProcessingDirectories();
        this.listenersRepository = new HashMap<>();
        registerDubboShutdownHook();
    }

    public FileSystemDynamicConfiguration(File rootDirectory, String encoding,
                                          String threadPoolPrefixName,
                                          int threadPoolSize,
                                          long keepAliveTime,
                                          ScopeModel scopeModel) {
        super(rootDirectory.getAbsolutePath(), threadPoolPrefixName, threadPoolSize, keepAliveTime, DEFAULT_GROUP, -1L);
        this.rootDirectory = rootDirectory;
        this.encoding = encoding;
        this.processingDirectories = initProcessingDirectories();
        this.listenersRepository = new HashMap<>();
        this.scopeModel = scopeModel;
        registerDubboShutdownHook();
    }

    public FileSystemDynamicConfiguration(URL url) {
        this(initDirectory(url), getEncoding(url), getThreadPoolPrefixName(url), getThreadPoolSize(url),
                getThreadPoolKeepAliveTime(url), url.getScopeModel());
    }

    private Set<File> initProcessingDirectories() {
        return isBasedPoolingWatchService() ? new LinkedHashSet<>() : emptySet();
    }

    public File configFile(String key, String group) {
        return new File(buildPathKey(group, key));
    }

    private void doInListener(String configFilePath, BiConsumer<File, List<ConfigurationListener>> consumer) {
        watchService.ifPresent(watchService -> {
            File configFile = new File(configFilePath);
            executeMutually(configFile.getParentFile(), () -> {
                // process the WatchEvents if not start
                if (!isProcessingWatchEvents()) {
                    processWatchEvents(watchService);
                }

                List<ConfigurationListener> listeners = getListeners(configFile);
                consumer.accept(configFile, listeners);

                // Nothing to return
                return null;
            });
        });
    }

    /**
     * Register the Dubbo ShutdownHook
     *
     * @since 2.7.8
     */
    private void registerDubboShutdownHook() {
        if (!hasRegisteredShutdownHook.compareAndSet(false, true)) {
            return;
        }
        ShutdownHookCallbacks shutdownHookCallbacks = ScopeModelUtil.getApplicationModel(scopeModel).getBeanFactory().getBean(ShutdownHookCallbacks.class);
        shutdownHookCallbacks.addCallback(() -> {
            watchService.ifPresent(w -> {
                try {
                    w.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            getWatchEventsLoopThreadPool().shutdown();
        });
    }

    private static boolean isProcessingWatchEvents() {
        return getWatchEventsLoopThreadPool().getActiveCount() > 0;
    }

    /**
     * Process the {@link WatchEvent WatchEvents} loop in async execution
     *
     * @param watchService {@link WatchService}
     */
    private void processWatchEvents(WatchService watchService) {
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
                                Path configDirectoryPath = (Path) watchKey.watchable();
                                Path currentPath = (Path) event.context();
                                Path configFilePath = configDirectoryPath.resolve(currentPath);
                                File configDirectory = configDirectoryPath.toFile();
                                executeMutually(configDirectory, () -> {
                                    fireConfigChangeEvent(configDirectory, configFilePath.toFile(), configChangeType);
                                    signalConfigDirectory(configDirectory);
                                    return null;
                                });
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

    private void signalConfigDirectory(File configDirectory) {
        if (isBasedPoolingWatchService()) {
            // remove configDirectory from processing set because it's done
            removeProcessingDirectory(configDirectory);
            // notify configDirectory
            notifyProcessingDirectory(configDirectory);
            if (logger.isDebugEnabled()) {
                logger.debug(format("The config rootDirectory[%s] is signalled...", configDirectory.getName()));
            }
        }
    }

    private void removeProcessingDirectory(File configDirectory) {
        processingDirectories.remove(configDirectory);
    }

    private void notifyProcessingDirectory(File configDirectory) {
        configDirectory.notifyAll();
    }

    private List<ConfigurationListener> getListeners(File configFile) {
        return listenersRepository.computeIfAbsent(configFile, p -> new LinkedList<>());
    }

    private void fireConfigChangeEvent(File configDirectory, File configFile, ConfigChangeType configChangeType) {
        String key = configFile.getName();
        String value = getConfig(configFile);
        // fire ConfigChangeEvent one by one
        getListeners(configFile).forEach(listener -> {
            try {
                listener.process(new ConfigChangedEvent(key, configDirectory.getName(), value, configChangeType));
            } catch (Throwable e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    private boolean canRead(File file) {
        return file.exists() && file.canRead();
    }

    @Override
    public Object getInternalProperty(String key) {
        return null;
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        return delay(pathKey, configFile -> {
            FileUtils.write(configFile, content, getEncoding());
            return true;
        });
    }

    @Override
    protected String doGetConfig(String pathKey) throws Exception {
        File configFile = new File(pathKey);
        return getConfig(configFile);
    }

    @Override
    protected boolean doRemoveConfig(String pathKey) throws Exception {
        delay(pathKey, configFile -> {
            String content = getConfig(configFile);
            FileUtils.deleteQuietly(configFile);
            return content;
        });
        return true;
    }

    @Override
    protected Collection<String> doGetConfigKeys(String groupPath) {
        File[] files = new File(groupPath).listFiles(File::isFile);
        if (files == null) {
            return new TreeSet<>();
        } else {
            return Stream.of(files)
                    .map(File::getName)
                    .collect(Collectors.toList());
        }
    }

    @Override
    protected void doAddListener(String pathKey, ConfigurationListener listener) {
        doInListener(pathKey, (configFilePath, listeners) -> {
            if (listeners.isEmpty()) { // If no element, it indicates watchService was registered before
                ThrowableConsumer.execute(configFilePath, configFile -> {
                    FileUtils.forceMkdirParent(configFile);
                    // A rootDirectory to be watched
                    File configDirectory = configFile.getParentFile();
                    if (configDirectory != null) {
                        // Register the configDirectory
                        configDirectory.toPath().register(watchService.get(), INTEREST_PATH_KINDS, MODIFIERS);
                    }
                });
            }
            // Add into cache
            listeners.add(listener);
        });
    }

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
        doInListener(pathKey, (file, listeners) -> {
            // Remove into cache
            listeners.remove(listener);
        });
    }

    /**
     * Delay action for {@link #configFile(String, String) config file}
     *
     * @param configFilePath the key to represent a configuration
     * @param function       the customized {@link Function function} with {@link File}
     * @param <V>            the computed value
     * @return
     */
    protected <V> V delay(String configFilePath, ThrowableFunction<File, V> function) {
        File configFile = new File(configFilePath);
        // Must be based on PoolingWatchService and has listeners under config file
        if (isBasedPoolingWatchService()) {
            File configDirectory = configFile.getParentFile();
            executeMutually(configDirectory, () -> {
                if (hasListeners(configFile) && isProcessing(configDirectory)) {
                    Integer delay = getDelay();
                    if (delay != null) {
                        // wait for delay in seconds
                        long timeout = SECONDS.toMillis(delay);
                        if (logger.isDebugEnabled()) {
                            logger.debug(format("The config[path : %s] is about to delay in %d ms.",
                                    configFilePath, timeout));
                        }
                        configDirectory.wait(timeout);
                    }
                }
                addProcessing(configDirectory);
                return null;
            });
        }

        V value = null;

        try {
            value = function.apply(configFile);
        } catch (Throwable e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }

        return value;
    }

    private boolean hasListeners(File configFile) {
        return getListeners(configFile).size() > 0;
    }

    /**
     * Is processing on {@link #buildGroupPath(String) config rootDirectory}
     *
     * @param configDirectory {@link #buildGroupPath(String) config rootDirectory}
     * @return if processing , return <code>true</code>, or <code>false</code>
     */
    private boolean isProcessing(File configDirectory) {
        return processingDirectories.contains(configDirectory);
    }

    private void addProcessing(File configDirectory) {
        processingDirectories.add(configDirectory);
    }

    public Set<String> getConfigGroups() {
        return Stream.of(getRootDirectory().listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    protected String getConfig(File configFile) {
        return ThrowableFunction.execute(configFile,
                file -> canRead(configFile) ? readFileToString(configFile, getEncoding()) : null);
    }

    @Override
    protected void doClose() throws Exception {

    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String getEncoding() {
        return encoding;
    }

    protected Integer getDelay() {
        return DELAY;
    }

    /**
     * It's whether the implementation of {@link WatchService} is based on {@linkplain sun.nio.fs.PollingWatchService}
     * or not.
     * <p>
     *
     * @return if based, return <code>true</code>, or <code>false</code>
     * @see #detectPoolingBasedWatchService(Optional)
     */
    protected static boolean isBasedPoolingWatchService() {
        return BASED_POOLING_WATCH_SERVICE;
    }

    protected static ThreadPoolExecutor getWatchEventsLoopThreadPool() {
        return WATCH_EVENTS_LOOP_THREAD_POOL;
    }

    protected ThreadPoolExecutor getWorkersThreadPool() {
        return super.getWorkersThreadPool();
    }

    private <V> V executeMutually(final Object mutex, Callable<V> callable) {
        V value = null;
        synchronized (mutex) {
            try {
                value = callable.call();
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return value;
    }

    private static <T> T[] of(T... values) {
        return values;
    }

    private static Integer initDelay(WatchEvent.Modifier[] modifiers) {
        if (isBasedPoolingWatchService()) {
            return 2;
        } else {
            return null;
        }
    }

    private static WatchEvent.Modifier[] initWatchEventModifiers() {
        return of();
    }

    /**
     * Detect the argument of {@link WatchService} is based on {@linkplain sun.nio.fs.PollingWatchService}
     * or not.
     * <p>
     * Some platforms do not provide the native implementation of {@link WatchService}, just use
     * {@linkplain sun.nio.fs.PollingWatchService} in periodic poll file modifications.
     *
     * @param watchService the instance of {@link WatchService}
     * @return if based, return <code>true</code>, or <code>false</code>
     */
    private static boolean detectPoolingBasedWatchService(Optional<WatchService> watchService) {
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

    protected static File initDirectory(URL url) {
        String directoryPath = getParameter(url, CONFIG_CENTER_DIR_PARAM_NAME, url == null ? null : url.getPath());
        File rootDirectory = null;
        if (!StringUtils.isBlank(directoryPath)) {
            rootDirectory = new File("/" + directoryPath);
        }

        if (directoryPath == null || !rootDirectory.exists()) { // If the directory does not exist
            rootDirectory = new File(DEFAULT_CONFIG_CENTER_DIR_PATH);
        }

        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw new IllegalStateException(format("Dubbo config center rootDirectory[%s] can't be created!",
                    rootDirectory.getAbsolutePath()));
        }
        return rootDirectory;
    }

    protected static String getEncoding(URL url) {
        return getParameter(url, CONFIG_CENTER_ENCODING_PARAM_NAME, DEFAULT_CONFIG_CENTER_ENCODING);
    }

    private static ThreadPoolExecutor newWatchEventsLoopThreadPool() {
        return new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE,
                0L, MILLISECONDS,
                new SynchronousQueue(),
                new NamedThreadFactory("dubbo-config-center-watch-events-loop", true));
    }
}
