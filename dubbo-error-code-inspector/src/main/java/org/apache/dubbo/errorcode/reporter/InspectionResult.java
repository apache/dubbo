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

import org.apache.dubbo.errorcode.model.LoggerMethodInvocation;
import org.apache.dubbo.errorcode.model.MethodDefinition;

import java.util.List;
import java.util.Map;

/**
 * Inspection results to Reporters.
 */
public class InspectionResult {
    private List<String> allErrorCodes;

    private List<String> linkNotReachableErrorCodes;

    private Map<String, List<MethodDefinition>> illegalInvocations;

    private Map<String, List<LoggerMethodInvocation>> invalidLoggerMethodInvocationLocations;

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

    public Map<String, List<MethodDefinition>> getIllegalInvocations() {
        return illegalInvocations;
    }

    public void setIllegalInvocations(Map<String, List<MethodDefinition>> illegalInvocations) {
        this.illegalInvocations = illegalInvocations;
    }

    public Map<String, List<LoggerMethodInvocation>> getInvalidLoggerMethodInvocationLocations() {
        return invalidLoggerMethodInvocationLocations;
    }

    public void setInvalidLoggerMethodInvocationLocations(Map<String, List<LoggerMethodInvocation>> invalidLoggerMethodInvocationLocations) {
        this.invalidLoggerMethodInvocationLocations = invalidLoggerMethodInvocationLocations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InspectionResult that = (InspectionResult) o;

        if (!allErrorCodes.equals(that.allErrorCodes)) return false;
        if (!linkNotReachableErrorCodes.equals(that.linkNotReachableErrorCodes)) return false;
        if (!illegalInvocations.equals(that.illegalInvocations)) return false;
        return invalidLoggerMethodInvocationLocations.equals(that.invalidLoggerMethodInvocationLocations);
    }

    @Override
    public int hashCode() {
        int result = allErrorCodes.hashCode();
        result = 31 * result + linkNotReachableErrorCodes.hashCode();
        result = 31 * result + illegalInvocations.hashCode();
        result = 31 * result + invalidLoggerMethodInvocationLocations.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "InspectionResult{" +
            "allErrorCodes=" + allErrorCodes +
            ", linkNotReachableErrorCodes=" + linkNotReachableErrorCodes +
            ", illegalInvocations=" + illegalInvocations +
            ", invalidLoggerMethodInvocationLocations=" + invalidLoggerMethodInvocationLocations +
            '}';
    }
}
