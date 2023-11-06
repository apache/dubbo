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

package org.apache.dubbo.config;

import java.io.Serializable;

/**
 * Configuration for triple protocol.
 */
public class TripleConfig implements Serializable {

    private static final long serialVersionUID = -3682252713701362155L;

    /**
     * The header table size.
     */
    private String headerTableSize;

    /**
     * Whether to enable push, default is false.
     */
    private Boolean enablePush;

    /**
     * Maximum concurrent streams.
     */
    private String maxConcurrentStreams;

    /**
     * Initial window size.
     */
    private String initialWindowSize;

    /**
     * Maximum frame size.
     */
    private String maxFrameSize;

    /**
     * Maximum header list size.
     */
    private String maxHeaderListSize;

    public String getHeaderTableSize() {
        return headerTableSize;
    }

    public void setHeaderTableSize(String headerTableSize) {
        this.headerTableSize = headerTableSize;
    }

    public Boolean getEnablePush() {
        return enablePush;
    }

    public void setEnablePush(Boolean enablePush) {
        this.enablePush = enablePush;
    }

    public String getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    public void setMaxConcurrentStreams(String maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    public String getInitialWindowSize() {
        return initialWindowSize;
    }

    public void setInitialWindowSize(String initialWindowSize) {
        this.initialWindowSize = initialWindowSize;
    }

    public String getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(String maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public String getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    public void setMaxHeaderListSize(String maxHeaderListSize) {
        this.maxHeaderListSize = maxHeaderListSize;
    }
}
