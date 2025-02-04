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
package org.apache.dubbo.config.nested;

import java.io.Serializable;

public class ServletConfig implements Serializable {

    private static final long serialVersionUID = 1091478303358670173L;

    /**
     * Whether to enable servlet support, requests are transport through the servlet container
     * <p>The default value is false.
     */
    private Boolean enabled;

    /**
     * Maximum concurrent streams.
     * <p>For HTTP/2
     * <p>Note that the default value for tomcat is 20. Highly recommended to change it to {@link Integer#MAX_VALUE}
     * <p>If set to zero or a negative number, the actual value will be set to {@link Integer#MAX_VALUE}.
     */
    private Integer maxConcurrentStreams;

    /**
     * The URL patterns that the servlet filter will be registered for.
     * <p>The default value is '/*'.
     */
    private String[] filterUrlPatterns;

    /**
     * The order of the servlet filter.
     * <p>The default value is -1000000.
     */
    private Integer filterOrder;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    public void setMaxConcurrentStreams(Integer maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    public String[] getFilterUrlPatterns() {
        return filterUrlPatterns;
    }

    public void setFilterUrlPatterns(String[] filterUrlPatterns) {
        this.filterUrlPatterns = filterUrlPatterns;
    }

    public Integer getFilterOrder() {
        return filterOrder;
    }

    public void setFilterOrder(Integer filterOrder) {
        this.filterOrder = filterOrder;
    }
}
