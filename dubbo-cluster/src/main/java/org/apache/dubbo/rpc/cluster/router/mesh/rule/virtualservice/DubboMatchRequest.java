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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboAttachmentMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.DubboMethodMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.util.TracingContextProvider;

import java.util.Map;
import java.util.Set;


public class DubboMatchRequest {
    private String name;
    private DubboMethodMatch method;
    private Map<String, String> sourceLabels;
    private DubboAttachmentMatch attachments;
    private Map<String, StringMatch> headers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DubboMethodMatch getMethod() {
        return method;
    }

    public void setMethod(DubboMethodMatch method) {
        this.method = method;
    }

    public Map<String, String> getSourceLabels() {
        return sourceLabels;
    }

    public void setSourceLabels(Map<String, String> sourceLabels) {
        this.sourceLabels = sourceLabels;
    }

    public DubboAttachmentMatch getAttachments() {
        return attachments;
    }

    public void setAttachments(DubboAttachmentMatch attachments) {
        this.attachments = attachments;
    }

    public Map<String, StringMatch> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, StringMatch> headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "DubboMatchRequest{" +
            "name='" + name + '\'' +
            ", method=" + method +
            ", sourceLabels=" + sourceLabels +
            ", attachments=" + attachments +
            ", headers=" + headers +
            '}';
    }

    public boolean isMatch(Invocation invocation, Map<String, String> sourceLabels, Set<TracingContextProvider> contextProviders) {
        // Match method
        if (getMethod() != null) {
            if (!getMethod().isMatch(invocation)) {
                return false;
            }
        }

        // Match Source Labels
        if (getSourceLabels() != null) {
            for (Map.Entry<String, String> entry : getSourceLabels().entrySet()) {
                String value = sourceLabels.get(entry.getKey());
                if (!entry.getValue().equals(value)) {
                    return false;
                }
            }
        }

        // Match attachment
        if (getAttachments() != null) {
            return getAttachments().isMatch(invocation, contextProviders);
        }

        // TODO Match headers

        return true;
    }
}
