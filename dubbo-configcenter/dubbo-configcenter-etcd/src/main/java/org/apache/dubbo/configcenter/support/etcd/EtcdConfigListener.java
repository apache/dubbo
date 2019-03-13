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

import io.etcd.jetcd.Watch;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import org.apache.dubbo.configcenter.ConfigChangeEvent;
import org.apache.dubbo.configcenter.ConfigurationListener;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class listens to Etcd changes and notify the
 * {@link org.apache.dubbo.configcenter.ConfigurationListener}
 */
public class EtcdConfigListener implements Watch.Listener {

    private ConfigurationListener listener;

    public EtcdConfigListener(ConfigurationListener listener) {
        this.listener = listener;
    }

    @Override
    public void onNext(WatchResponse response) {
        for (WatchEvent etcdEvent : response.getEvents()) {
            ConfigChangeEvent event = new ConfigChangeEvent(
                    etcdEvent.getKeyValue().getKey().toString(UTF_8),
                    etcdEvent.getKeyValue().getValue().toString(UTF_8));
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
}
