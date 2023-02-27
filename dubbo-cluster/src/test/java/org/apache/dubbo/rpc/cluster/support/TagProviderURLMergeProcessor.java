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
import org.apache.dubbo.rpc.cluster.ProviderURLMergeProcessor;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;

public class TagProviderURLMergeProcessor implements ProviderURLMergeProcessor {

    @Override
    public URL mergeUrl(URL remoteUrl, Map<String, String> localParametersMap) {
        String tag = localParametersMap.get(TAG_KEY);
        remoteUrl = remoteUrl.removeParameter(TAG_KEY);
        remoteUrl = remoteUrl.addParameter(TAG_KEY, tag);
        return remoteUrl;
    }

    @Override
    public Map<String, String> mergeLocalParams(Map<String, String> localMap) {
        localMap.remove(TAG_KEY);
        return ProviderURLMergeProcessor.super.mergeLocalParams(localMap);
    }
}
