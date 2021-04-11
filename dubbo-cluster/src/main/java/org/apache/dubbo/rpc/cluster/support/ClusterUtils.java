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
package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * ClusterUtils
 */
public class ClusterUtils {

    private ClusterUtils() {
    }

    public static URL mergeUrl(URL remoteUrl, Map<String, String> localMap) {
        Map<String, String> map = new HashMap<String, String>();
        Map<String, String> remoteMap = remoteUrl.getParameters();

        if (remoteMap != null && remoteMap.size() > 0) {
            map.putAll(remoteMap);

            // Remove configurations from provider, some items should be affected by provider.
            map.remove(THREAD_NAME_KEY);
            map.remove(DEFAULT_KEY_PREFIX + THREAD_NAME_KEY);

            map.remove(THREADPOOL_KEY);
            map.remove(DEFAULT_KEY_PREFIX + THREADPOOL_KEY);

            map.remove(CORE_THREADS_KEY);
            map.remove(DEFAULT_KEY_PREFIX + CORE_THREADS_KEY);

            map.remove(THREADS_KEY);
            map.remove(DEFAULT_KEY_PREFIX + THREADS_KEY);

            map.remove(QUEUES_KEY);
            map.remove(DEFAULT_KEY_PREFIX + QUEUES_KEY);

            map.remove(ALIVE_KEY);
            map.remove(DEFAULT_KEY_PREFIX + ALIVE_KEY);

            map.remove(Constants.TRANSPORTER_KEY);
            map.remove(DEFAULT_KEY_PREFIX + Constants.TRANSPORTER_KEY);
        }

        if (localMap != null && localMap.size() > 0) {
            Map<String, String> copyOfLocalMap = new HashMap<>(localMap);

            if(map.containsKey(GROUP_KEY)){
                copyOfLocalMap.remove(GROUP_KEY);
            }
            if(map.containsKey(VERSION_KEY)){
                copyOfLocalMap.remove(VERSION_KEY);
            }

            copyOfLocalMap.remove(RELEASE_KEY);
            copyOfLocalMap.remove(DUBBO_VERSION_KEY);
            copyOfLocalMap.remove(METHODS_KEY);
            copyOfLocalMap.remove(TIMESTAMP_KEY);
            copyOfLocalMap.remove(TAG_KEY);

            map.putAll(copyOfLocalMap);

            map.put(REMOTE_APPLICATION_KEY, remoteMap.get(APPLICATION_KEY));

            // Combine filters and listeners on Provider and Consumer
            String remoteFilter = remoteMap.get(REFERENCE_FILTER_KEY);
            String localFilter = copyOfLocalMap.get(REFERENCE_FILTER_KEY);
            if (remoteFilter != null && remoteFilter.length() > 0
                    && localFilter != null && localFilter.length() > 0) {
                map.put(REFERENCE_FILTER_KEY, remoteFilter + "," + localFilter);
            }
            String remoteListener = remoteMap.get(INVOKER_LISTENER_KEY);
            String localListener = copyOfLocalMap.get(INVOKER_LISTENER_KEY);
            if (remoteListener != null && remoteListener.length() > 0
                    && localListener != null && localListener.length() > 0) {
                map.put(INVOKER_LISTENER_KEY, remoteListener + "," + localListener);
            }
        }

        return remoteUrl.clearParameters().addParameters(map);
    }

    public static URL mergeProviderUrl(URL remoteUrl, Map<String, String> localMap) {

        //urlprocessor => upc
        List<ProviderURLMergeProcessor> providerURLMergeProcessors = ExtensionLoader.getExtensionLoader(ProviderURLMergeProcessor.class)
                .getActivateExtension(remoteUrl, "upc");

        if (providerURLMergeProcessors != null && providerURLMergeProcessors.size() > 0) {
            for (ProviderURLMergeProcessor providerURLMergeProcessor : providerURLMergeProcessors) {
                if (providerURLMergeProcessor.accept(remoteUrl, localMap)) {
                    return providerURLMergeProcessor.mergeProviderUrl(remoteUrl, localMap);
                }
            }
        }

        return mergeUrl(remoteUrl, localMap);
    }

}
