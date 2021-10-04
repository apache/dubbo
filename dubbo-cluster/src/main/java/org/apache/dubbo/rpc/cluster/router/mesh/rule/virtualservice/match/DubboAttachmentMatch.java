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

import java.util.Map;


public class DubboAttachmentMatch {
    private Map<String, StringMatch> eagleeyecontext;
    private Map<String, StringMatch> dubbocontext;

    public Map<String, StringMatch> getEagleeyecontext() {
        return eagleeyecontext;
    }

    public void setEagleeyecontext(Map<String, StringMatch> eagleeyecontext) {
        this.eagleeyecontext = eagleeyecontext;
    }

    public Map<String, StringMatch> getDubbocontext() {
        return dubbocontext;
    }

    public void setDubbocontext(Map<String, StringMatch> dubbocontext) {
        this.dubbocontext = dubbocontext;
    }

    public static boolean isMatch(DubboAttachmentMatch dubboAttachmentMatch, Map<String, String> eagleeyeContext, Map<String, String> dubboContext) {
        if (dubboAttachmentMatch.getDubbocontext() != null) {
            for (Map.Entry<String, StringMatch> stringStringMatchEntry : dubboAttachmentMatch.getDubbocontext().entrySet()) {
                String key = stringStringMatchEntry.getKey();
                StringMatch stringMatch = stringStringMatchEntry.getValue();

                String dubboContextValue = dubboContext.get(key);
                if (dubboContextValue == null) {
                    return false;
                }
                if (!StringMatch.isMatch(stringMatch, dubboContextValue)) {
                    return false;
                }
            }
        }

        if (dubboAttachmentMatch.getEagleeyecontext() != null) {
            for (Map.Entry<String, StringMatch> stringStringMatchEntry : dubboAttachmentMatch.getEagleeyecontext().entrySet()) {
                String key = stringStringMatchEntry.getKey();
                StringMatch stringMatch = stringStringMatchEntry.getValue();

                String eagleeyeContextValue = eagleeyeContext.get(key);
                if (eagleeyeContextValue == null) {
                    return false;
                }
                if (!StringMatch.isMatch(stringMatch, eagleeyeContextValue)) {
                    return false;
                }
            }
        }

        return true;
    }
}
