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
package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.event.DubboEvent;
import org.apache.dubbo.common.utils.TimePair;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

public class DirectoryRefreshEvent extends DubboEvent {

    private final TimePair timePair;

    private final Summary summary;

    private final Map<String, String> attachments;

    public DirectoryRefreshEvent(ApplicationModel applicationModel, Summary summary, Map<String, String> attachments) {
        super(applicationModel);
        this.summary = summary;
        this.attachments = attachments;
        this.timePair = TimePair.start();
    }

    public Summary getSummary() {
        return summary;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public TimePair getTimePair() {
        return timePair;
    }

    public static class Summary {

        public Map<String, Integer> directoryNumValidMap;

        public Map<String, Integer> directoryNumDisableMap;

        public Map<String, Integer> directoryNumToReConnectMap;

        public Map<String, Integer> directoryNumAllMap;
    }
}
