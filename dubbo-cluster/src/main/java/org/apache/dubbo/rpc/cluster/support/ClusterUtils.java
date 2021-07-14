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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.cluster.ProviderURLMergeProcessor;

import java.util.Map;

import static org.apache.dubbo.config.Constants.URL_MERGE_PROCESSOR_KEY;


/**
 * ClusterUtils
 */
public class ClusterUtils {

    private ClusterUtils() {
    }

    public static URL mergeUrl(URL remoteUrl, Map<String, String> localMap) {

        String ump = localMap.get(URL_MERGE_PROCESSOR_KEY);
        ProviderURLMergeProcessor providerURLMergeProcessor;

        if (StringUtils.isNotEmpty(ump)) {
            providerURLMergeProcessor = ExtensionLoader.getExtensionLoader(ProviderURLMergeProcessor.class).getExtension(ump);
        } else {
            providerURLMergeProcessor = ExtensionLoader.getExtensionLoader(ProviderURLMergeProcessor.class).getExtension("default");
        }

        return providerURLMergeProcessor.mergeUrl(remoteUrl, localMap);
    }

}
