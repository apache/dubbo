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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.Constants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.rpc.Constants.REFERENCE_FILTER_KEY;

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
            // All providers come to here have been filtered by group, which means only those providers that have the exact same group value with the consumer could come to here.
            // So, generally, we don't need to care about the group value here.
            // But when comes to group merger, there is an exception, the consumer group may be '*' while the provider group can be empty or any other values.
            String remoteGroup = map.get(GROUP_KEY);
            String remoteRelease = map.get(RELEASE_KEY);
            map.putAll(localMap);
            if (StringUtils.isNotEmpty(remoteGroup)) {
                map.put(GROUP_KEY, remoteGroup);
            }
            // we should always keep the Provider RELEASE_KEY not overrode by the the value on Consumer side.
            map.remove(RELEASE_KEY);
            if (StringUtils.isNotEmpty(remoteRelease)) {
                map.put(RELEASE_KEY, remoteRelease);
            }
        }
        if (remoteMap != null && remoteMap.size() > 0) {
            // Use version passed from provider side
            reserveRemoteValue(DUBBO_VERSION_KEY, map, remoteMap);
            reserveRemoteValue(VERSION_KEY, map, remoteMap);
            reserveRemoteValue(METHODS_KEY, map, remoteMap);
            reserveRemoteValue(TIMESTAMP_KEY, map, remoteMap);
            reserveRemoteValue(TAG_KEY, map, remoteMap);
            // TODO, for compatibility consideration, we cannot simply change the value behind APPLICATION_KEY from Consumer to Provider. So just add an extra key here.
            // Reserve application name from provider.
            map.put(REMOTE_APPLICATION_KEY, remoteMap.get(APPLICATION_KEY));

            // Combine filters and listeners on Provider and Consumer
            String remoteFilter = remoteMap.get(REFERENCE_FILTER_KEY);
            String localFilter = localMap.get(REFERENCE_FILTER_KEY);
            if (remoteFilter != null && remoteFilter.length() > 0
                    && localFilter != null && localFilter.length() > 0) {
                localMap.put(REFERENCE_FILTER_KEY, remoteFilter + "," + localFilter);
            }
            String remoteListener = remoteMap.get(INVOKER_LISTENER_KEY);
            String localListener = localMap.get(INVOKER_LISTENER_KEY);
            if (remoteListener != null && remoteListener.length() > 0
                    && localListener != null && localListener.length() > 0) {
                localMap.put(INVOKER_LISTENER_KEY, remoteListener + "," + localListener);
            }
        }

        return remoteUrl.clearParameters().addParameters(map);
    }

    private static void reserveRemoteValue(String key, Map<String, String> map, Map<String, String> remoteMap) {
        String remoteValue = remoteMap.get(key);
        if (StringUtils.isNotEmpty(remoteValue)) {
            map.put(key, remoteValue);
        }
    }

}
