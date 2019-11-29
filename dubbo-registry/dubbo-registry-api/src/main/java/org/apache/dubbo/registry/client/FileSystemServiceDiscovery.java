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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration;
import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.event.EventListener;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.alibaba.fastjson.JSON.toJSONString;
import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static org.apache.dubbo.common.config.configcenter.DynamicConfiguration.DEFAULT_GROUP;
import static org.apache.dubbo.common.config.configcenter.file.FileSystemDynamicConfiguration.CONFIG_CENTER_DIR_PARAM_NAME;

/**
 * File System {@link ServiceDiscovery} implementation
 *
 * @see FileSystemDynamicConfiguration
 * @since 2.7.5
 */
public class FileSystemServiceDiscovery implements ServiceDiscovery, EventListener<ServiceInstancesChangedEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<File, FileLock> fileLocksCache = new ConcurrentHashMap<>();

    private FileSystemDynamicConfiguration dynamicConfiguration;

    @Override
    public void onEvent(ServiceInstancesChangedEvent event) {

    }

    @Override
    public void initialize(URL registryURL) throws Exception {
        dynamicConfiguration = createDynamicConfiguration(registryURL);
        registerDubboShutdownHook();
        registerListener();
    }

    private void registerDubboShutdownHook() {
        ShutdownHookCallbacks.INSTANCE.addCallback(this::destroy);
    }

    private void registerListener() {
        getServices().forEach(serviceName -> {
            dynamicConfiguration.getConfigKeys(DEFAULT_GROUP).forEach(serviceInstanceId -> {
                dynamicConfiguration.addListener(serviceInstanceId, serviceName, this::onConfigChanged);
            });
        });
    }

    public void onConfigChanged(ConfigChangedEvent event) {

    }

    @Override
    public void destroy() throws Exception {
        dynamicConfiguration.close();
        releaseAndRemoveRegistrationFiles();
    }

    private void releaseAndRemoveRegistrationFiles() {
        fileLocksCache.keySet().forEach(file -> {
            releaseFileLock(file);
            removeFile(file);
        });
    }

    private void removeFile(File file) {
        FileUtils.deleteQuietly(file);
    }

    private String getServiceInstanceId(ServiceInstance serviceInstance) {
        String id = serviceInstance.getId();
        if (StringUtils.isBlank(id)) {
            return serviceInstance.getHost() + "." + serviceInstance.getPort();
        }
        return id;
    }

    private String getServiceName(ServiceInstance serviceInstance) {
        return serviceInstance.getServiceName();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) {
        return dynamicConfiguration.getConfigKeys(DEFAULT_GROUP)
                .stream()
                .map(serviceInstanceId -> dynamicConfiguration.getConfig(serviceInstanceId, serviceName))
                .map(content -> JSON.parseObject(content, DefaultServiceInstance.class))
                .collect(Collectors.toList());
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        String serviceInstanceId = getServiceInstanceId(serviceInstance);
        String serviceName = getServiceName(serviceInstance);
        String content = toJSONString(serviceInstance);
        if (dynamicConfiguration.publishConfig(serviceInstanceId, serviceName, content)) {
            lockFile(serviceInstanceId, serviceName);
        }
    }

    private void lockFile(String serviceInstanceId, String serviceName) {
        File serviceInstanceFile = serviceInstanceFile(serviceInstanceId, serviceName);
        Path serviceInstanceFilePath = serviceInstanceFile.toPath();

        fileLocksCache.computeIfAbsent(serviceInstanceFile, file -> {
            FileLock fileLock = null;
            try {
                FileChannel fileChannel = open(serviceInstanceFilePath, StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                fileLock = fileChannel.tryLock();
            } catch (IOException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (fileLock != null) {
                if (logger.isInfoEnabled()) {
                    logger.info(format("%s has been locked", serviceInstanceFilePath.toAbsolutePath()));
                }
            }
            return fileLock;
        });
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        register(serviceInstance);
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        String key = getServiceInstanceId(serviceInstance);
        String group = getServiceName(serviceInstance);
        releaseFileLock(key, group);
        dynamicConfiguration.removeConfig(key, group);
    }

    private void releaseFileLock(String serviceInstanceId, String serviceName) {
        File serviceInstanceFile = serviceInstanceFile(serviceInstanceId, serviceName);
        releaseFileLock(serviceInstanceFile);
    }

    private void releaseFileLock(File serviceInstanceFile) {
        fileLocksCache.computeIfPresent(serviceInstanceFile, (f, fileLock) -> {
            releaseFileLock(fileLock);
            if (logger.isInfoEnabled()) {
                logger.info(format("The file[%s] has been released", serviceInstanceFile.getAbsolutePath()));
            }
            return null;
        });
    }

    private void releaseFileLock(FileLock fileLock) {
        try (FileChannel fileChannel = fileLock.channel()) {
            fileLock.release();
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private File serviceInstanceFile(String serviceInstanceId, String serviceName) {
        return dynamicConfiguration.configFile(serviceInstanceId, serviceName);
    }

    @Override
    public Set<String> getServices() {
        return dynamicConfiguration.getConfigGroups();
    }

    private static FileSystemDynamicConfiguration createDynamicConfiguration(URL connectionURL) {
        String path = System.getProperty("user.home") + File.separator + ".dubbo" + File.separator + "registry";
        return new FileSystemDynamicConfiguration(connectionURL.addParameter(CONFIG_CENTER_DIR_PARAM_NAME, path));
    }
}
