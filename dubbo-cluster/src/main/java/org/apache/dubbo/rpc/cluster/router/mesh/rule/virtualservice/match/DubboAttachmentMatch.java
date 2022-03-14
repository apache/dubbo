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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;

import java.util.Map;
import java.util.Set;


public class DubboAttachmentMatch {
    private Map<String, StringMatch> tracingContext;
    private Map<String, StringMatch> dubboContext;

    public Map<String, StringMatch> getTracingContext() {
        return tracingContext;
    }

    public void setTracingContext(Map<String, StringMatch> tracingContext) {
        this.tracingContext = tracingContext;
    }

    public Map<String, StringMatch> getDubboContext() {
        return dubboContext;
    }

    public void setDubboContext(Map<String, StringMatch> dubboContext) {
        this.dubboContext = dubboContext;
    }

    public boolean isMatch(Invocation invocation, Set<TracingContextProvider> contextProviders) {
        // Match Dubbo Context
        if (dubboContext != null) {
            for (Map.Entry<String, StringMatch> entry : dubboContext.entrySet()) {
                String key = entry.getKey();
                if(!entry.getValue().isMatch(invocation.getAttachment(key))) {
                    return false;
                }
            }
        }

        // Match Tracing Context
        if (tracingContext != null) {
            for (Map.Entry<String, StringMatch> entry : tracingContext.entrySet()) {
                String key = entry.getKey();
                boolean match = false;
                for (TracingContextProvider contextProvider : contextProviders) {
                    if (entry.getValue().isMatch(contextProvider.getValue(invocation, key))) {
                        match = true;
                    }
                }
                if (!match) {
                    return false;
                }
            }
        }
        return true;
    }
}
