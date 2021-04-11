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

package org.apache.dubbo.configcenter.support.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.etcd.StateListener;
import org.apache.dubbo.remoting.etcd.jetcd.JEtcdClient;

import com.google.protobuf.ByteString;
import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.api.WatchCancelRequest;
import io.etcd.jetcd.api.WatchCreateRequest;
import io.etcd.jetcd.api.WatchGrpc;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.dubbo.common.constants.CommonConstants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;

/**
 * The etcd implementation of {@link DynamicConfiguration}
 */
public class EtcdDynamicConfiguration implements DynamicConfiguration {

    /**
     * The final root path would be: /$NAME_SPACE/config
     */
    private String rootPath;

    /**
     * The etcd client
     */
    private final JEtcdClient etcdClient;

    /**
     * The map store the key to {@link EtcdConfigWatcher} mapping
     */
    private final ConcurrentMap<ConfigurationListener, EtcdConfigWatcher> watchListenerMap;

    EtcdDynamicConfiguration(URL url) {
        rootPath = PATH_SEPARATOR + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + "/config";
        etcdClient = new JEtcdClient(url);
        etcdClient.addStateListener(state -> {
            if (state == StateListener.CONNECTED) {
                try {
                    recover();
                } catch (Exception e) {
                    // ignore
                }
            }
        });
        watchListenerMap = new ConcurrentHashMap<>();
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        if (watchListenerMap.get(listener) == null) {
            EtcdConfigWatcher watcher = new EtcdConfigWatcher(key, group, listener);
            watchListenerMap.put(listener, watcher);
            watcher.watch();
        }
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {
        EtcdConfigWatcher watcher = watchListenerMap.get(listener);
        watcher.cancelWatch();
    }

    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        return (String) getInternalProperty(convertKey(group, key));
    }

//    @Override
//    public String getConfigs(String key, String group, long timeout) throws IllegalStateException {
//        if (StringUtils.isEmpty(group)) {
//            group = DEFAULT_GROUP;
//        }
//        return (String) getInternalProperty(convertKey(group, key));
//    }

    @Override
    public Object getInternalProperty(String key) {
        return etcdClient.getKVValue(key);
    }

    private String buildPath(String group) {
        String actualGroup = StringUtils.isEmpty(group) ? DEFAULT_GROUP : group;
        return rootPath + PATH_SEPARATOR + actualGroup;
    }

    private String convertKey(String group, String key) {
        return buildPath(group) + PATH_SEPARATOR + key;
    }

    private void recover() {
        for (EtcdConfigWatcher watcher : watchListenerMap.values()) {
            watcher.watch();
        }
    }

    public class EtcdConfigWatcher implements StreamObserver<WatchResponse> {

        private ConfigurationListener listener;
        protected WatchGrpc.WatchStub watchStub;
        private StreamObserver<WatchRequest> observer;
        protected long watchId;
        private ManagedChannel channel;

        private final String key;

        private final String group;

        private String normalizedKey;

        public EtcdConfigWatcher(String key, String group, ConfigurationListener listener) {
            this.key = key;
            this.group = group;
            this.normalizedKey = convertKey(group, key);
            this.listener = listener;
            this.channel = etcdClient.getChannel();
        }

        @Override
        public void onNext(WatchResponse watchResponse) {
            this.watchId = watchResponse.getWatchId();
            for (Event etcdEvent : watchResponse.getEventsList()) {
                ConfigChangeType type = ConfigChangeType.MODIFIED;
                if (etcdEvent.getType() == Event.EventType.DELETE) {
                    type = ConfigChangeType.DELETED;
                }
                ConfigChangedEvent event = new ConfigChangedEvent(key, group,
                        etcdEvent.getKv().getValue().toString(UTF_8), type);
                listener.process(event);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // ignore
        }

        @Override
        public void onCompleted() {
            // ignore
        }

        public long getWatchId() {
            return watchId;
        }

        private void watch() {
            watchStub = WatchGrpc.newStub(channel);
            observer = watchStub.watch(this);
            WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
                    .setKey(ByteString.copyFromUtf8(normalizedKey))
                    .setProgressNotify(true);
            WatchRequest req = WatchRequest.newBuilder().setCreateRequest(builder).build();
            observer.onNext(req);
        }

        private void cancelWatch() {
            WatchCancelRequest watchCancelRequest =
                    WatchCancelRequest.newBuilder().setWatchId(watchId).build();
            WatchRequest cancelRequest = WatchRequest.newBuilder()
                    .setCancelRequest(watchCancelRequest).build();
            observer.onNext(cancelRequest);
        }
    }
}
