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

package org.apache.dubbo.errorcode.reporter;

import java.util.List;
import java.util.Objects;

/**
 * Inspection results to Reporters.
 */
public class ReportResult {
    private List<String> allErrorCodes;

    private List<String> linkNotReachableErrorCodes;

    // Generated methods, re-generate them if necessary.

    public List<String> getAllErrorCodes() {
        return allErrorCodes;
    }

    public void setAllErrorCodes(List<String> allErrorCodes) {
        this.allErrorCodes = allErrorCodes;
    }

    public List<String> getLinkNotReachableErrorCodes() {
        return linkNotReachableErrorCodes;
    }

    public void setLinkNotReachableErrorCodes(List<String> linkNotReachableErrorCodes) {
        this.linkNotReachableErrorCodes = linkNotReachableErrorCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReportResult that = (ReportResult) o;

        if (!Objects.equals(allErrorCodes, that.allErrorCodes))
            return false;
        return Objects.equals(linkNotReachableErrorCodes, that.linkNotReachableErrorCodes);
    }

    @Override
    public int hashCode() {
        int result = allErrorCodes != null ? allErrorCodes.hashCode() : 0;
        result = 31 * result + (linkNotReachableErrorCodes != null ? linkNotReachableErrorCodes.hashCode() : 0);
        return result;
    }
}
