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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.lang.Integer.max;
import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Stream.of;
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

    public static final String CONFIG_CENTER_THREAD_POOL_SIZE_PARAM_NAME = "dubbo.config-center.thread-pool.size";

    public static final String CONFIG_CENTER_ENCODING_PARAM_NAME = "dubbo.config-center.encoding";

    public static final String DEFAULT_CONFIG_CENTER_DIR_PATH = System.getProperty("user.home") + File.separator
            + ".dubbo" + File.separator + "config-center";

    public static final String DEFAULT_CONFIG_CENTER_THREAD_POOL_NAME = "dubbo-config-center";

    public static final int DEFAULT_CONFIG_CENTER_THREAD_POOL_SIZE = 2;

    public static final String DEFAULT_CONFIG_CENTER_ENCODING = "UTF-8";

    private static final WatchEvent.Kind[] INTEREST_PATH_KINDS =
            new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY};

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
     * Logger
     */
    private final Log logger = LogFactory.getLog(getClass());

    /**
     * The Root Directory for config center
     */
    private final File directory;

    /**
     * The thread pool used to execute I/O tasks
     */
    private final ExecutorService executorService;

    private final String encoding;

    private final Optional<WatchService> watchService;

    private final Map<Path, List<ConfigurationListener>> listenersRepository;

    public FileSystemDynamicConfiguration(URL url) {
        this.directory = initDirectory(url);
        this.executorService = initExecutorService(url);
        this.encoding = url.getParameter(CONFIG_CENTER_ENCODING_PARAM_NAME, DEFAULT_CONFIG_CENTER_ENCODING);
        this.watchService = newWatchService();
        this.listenersRepository = new LinkedHashMap<>();
    }

    private Optional<WatchService> newWatchService() {
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

    private File initDirectory(URL url) {
        String directoryPath = url.getParameter(CONFIG_CENTER_DIR_PARAM_NAME, DEFAULT_CONFIG_CENTER_DIR_PATH);
        File rootDirectory = new File(url.getParameter(CONFIG_CENTER_DIR_PARAM_NAME, DEFAULT_CONFIG_CENTER_DIR_PATH));
        if (!rootDirectory.exists() && !rootDirectory.mkdirs()) {
            throw new IllegalStateException(format("Dubbo config center directory[%s] can't be created!",
                    directoryPath));
        }
        return rootDirectory;
    }

    private ExecutorService initExecutorService(URL url) {
        String threadPoolName = url.getParameter(CONFIG_CENTER_THREAD_POOL_NAME_PARAM_NAME, DEFAULT_CONFIG_CENTER_THREAD_POOL_NAME);
        int threadPoolSize = url.getParameter(CONFIG_CENTER_THREAD_POOL_SIZE_PARAM_NAME, DEFAULT_CONFIG_CENTER_THREAD_POOL_SIZE);
        threadPoolSize = max(threadPoolSize, 2);
        return newFixedThreadPool(threadPoolSize, new NamedThreadFactory(threadPoolName));
    }

    private File groupDirectory(String group) {
        String actualGroup = isBlank(group) ? DEFAULT_GROUP : group;
        return new File(directory, actualGroup);
    }

    private File keyFile(String key, String group) {
        return new File(groupDirectory(group), key);
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        doInListener(key, group, (watchedFilePath, listeners) -> {

            if (listeners.isEmpty()) { // If no element, register watchService
                ThrowableConsumer.execute(watchedFilePath, path -> {
                    File file = path.toFile();
                    // A directory to be watched
                    File watchedDirectory = file.isFile() ? file.getParentFile() : file.isDirectory() ? file : null;
                    if (watchedDirectory != null) {
                        watchedDirectory.toPath().register(watchService.get(), INTEREST_PATH_KINDS);
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

    private void doInListener(String key, String group, BiConsumer<Path, List<ConfigurationListener>> consumer) {
        watchService.ifPresent(watchService -> {
            // Initializes watchService if there is not any listener
            if (listenersRepository.isEmpty()) {
                initWatchService(watchService);
            }

            File keyFile = keyFile(key, group);
            if (keyFile.exists()) { // keyFile must exist
                Path watchedFilePath = keyFile.toPath();
                List<ConfigurationListener> listeners = getListeners(watchedFilePath);
                consumer.accept(watchedFilePath, listeners);
            }
        });
    }

    private void initWatchService(WatchService watchService) {
        executorService.submit(() -> { // Async execution
            while (true) {
                WatchKey watchKey = watchService.poll();
                if (watchKey != null && watchKey.isValid()) {
                    List<WatchEvent<?>> events = watchKey.pollEvents();
                    for (WatchEvent event : events) {
                        WatchEvent.Kind kind = event.kind();
                        // configChangeType's key to match WatchEvent's Kind
                        ConfigChangeType configChangeType = CONFIG_CHANGE_TYPES_MAP.get(kind.name());
                        if (configChangeType != null) {
                            Path parentPath = (Path) watchKey.watchable();
                            Path currentPath = (Path) event.context();
                            fireConfigChangeEvent(parentPath.resolve(currentPath), configChangeType);
                        }
                    }
                }
            }
        });
    }

    private List<ConfigurationListener> getListeners(Path watchedFilePath) {
        return listenersRepository.computeIfAbsent(watchedFilePath, p -> new LinkedList<>());
    }

    private void fireConfigChangeEvent(Path watchedFilePath, ConfigChangeType configChangeType) {
        File watchedFile = watchedFilePath.toFile();
        String key = watchedFile.getName();
        String group = watchedFile.getParentFile().getName();
        String value = getConfig(key, group);
        // fire ConfigChangeEvent one by one
        getListeners(watchedFilePath).forEach(listener -> {
            listener.process(new ConfigChangeEvent(key, value, configChangeType));
        });
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        File keyFile = keyFile(key, group);
        return keyFile.exists() ? execute(() -> readFileToString(keyFile, encoding), timeout) : null;
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
        File keyFile = keyFile(key, group);
        boolean published = false;
        try {
            FileUtils.write(keyFile, content, encoding);
            published = true;
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
        return published;
    }

    @Override
    public Set<String> getConfigKeys(String group) throws UnsupportedOperationException {
        return of(groupDirectory(group).listFiles(File::isFile))
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
                Future<V> future = executorService.submit(task);
                value = future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return value;
    }

    protected File getDirectory() {
        return directory;
    }

    protected ExecutorService getExecutorService() {
        return executorService;
    }

    protected String getEncoding() {
        return encoding;
    }
}
