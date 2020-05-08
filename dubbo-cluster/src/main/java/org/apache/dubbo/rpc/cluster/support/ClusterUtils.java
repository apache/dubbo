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

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ALIVE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CORE_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INVOKER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.QUEUES_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isNotEmptyMap;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;
import static org.apache.dubbo.remoting.Constants.TRANSPORTER_KEY;

/**
 * ClusterUtils
 */
public class ClusterUtils {

    private ClusterUtils() {
    }

    public static URL mergeUrl(URL remoteUrl, Map<String, String> localMap) {
        Map<String, String> remoteMap = remoteUrl.getParameters();

        if (remoteMap == null || remoteMap.size() <= 0) {
            return remoteUrl.addParameters(localMap);
        }

        // Remove configurations from provider, some items should not being affected by provider.
        remoteMap.remove(THREAD_NAME_KEY);
        remoteMap.remove(THREADPOOL_KEY);
        remoteMap.remove(CORE_THREADS_KEY);
        remoteMap.remove(THREADS_KEY);
        remoteMap.remove(QUEUES_KEY);
        remoteMap.remove(ALIVE_KEY);
        remoteMap.remove(TRANSPORTER_KEY);

        remoteMap.put(REMOTE_APPLICATION_KEY, remoteMap.get(APPLICATION_KEY));

        if (isNotEmptyMap(localMap)) {
            remoteMap.putAll(localMap);

            // Combine filters and listeners on Provider and Consumer
            String remoteFilter = remoteMap.get(REFERENCE_FILTER_KEY);
            String localFilter = localMap.get(REFERENCE_FILTER_KEY);
            if (isNotEmpty(remoteFilter) && isNotEmpty(localFilter)) {
                remoteMap.put(REFERENCE_FILTER_KEY, remoteFilter + "," + localFilter);
            }
            String remoteListener = remoteMap.get(INVOKER_LISTENER_KEY);
            String localListener = localMap.get(INVOKER_LISTENER_KEY);
            if (isNotEmpty(remoteListener) && isNotEmpty(localListener)) {
                remoteMap.put(INVOKER_LISTENER_KEY, remoteListener + "," + localListener);
            }
        }

        return remoteUrl;
    }

}
