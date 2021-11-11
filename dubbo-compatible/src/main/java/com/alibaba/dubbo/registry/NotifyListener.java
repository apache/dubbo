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

package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public interface NotifyListener {

    void notify(List<URL> urls);

    class CompatibleNotifyListener implements NotifyListener {

        private org.apache.dubbo.registry.NotifyListener listener;

        public CompatibleNotifyListener(org.apache.dubbo.registry.NotifyListener listener) {
            this.listener = listener;
        }

        @Override
        public void notify(List<URL> urls) {
            if (listener != null) {
                listener.notify(urls.stream().map(url -> url.getOriginalURL()).collect(Collectors.toList()));
            }
        }
    }

    class ReverseCompatibleNotifyListener implements org.apache.dubbo.registry.NotifyListener {

        private NotifyListener listener;

        public ReverseCompatibleNotifyListener(NotifyListener listener) {
            this.listener = listener;
        }

        @Override
        public void notify(List<org.apache.dubbo.common.URL> urls) {
            if (listener != null) {
                listener.notify(urls.stream().map(url -> new URL(url)).collect(Collectors.toList()));
            }
        }
    }
}
