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
package org.apache.dubbo.xds.resource_new.cluster;

import org.apache.dubbo.common.lang.Nullable;

public class SuccessRateEjection {

    @Nullable
    private final Integer stdevFactor;

    @Nullable
    private final Integer enforcementPercentage;

    @Nullable
    private final Integer minimumHosts;

    @Nullable
    private final Integer requestVolume;

    public static SuccessRateEjection create(
            @Nullable Integer stdevFactor,
            @Nullable Integer enforcementPercentage,
            @Nullable Integer minimumHosts,
            @Nullable Integer requestVolume) {
        return new SuccessRateEjection(stdevFactor, enforcementPercentage, minimumHosts, requestVolume);
    }

    public SuccessRateEjection(
            @Nullable Integer stdevFactor,
            @Nullable Integer enforcementPercentage,
            @Nullable Integer minimumHosts,
            @Nullable Integer requestVolume) {
        this.stdevFactor = stdevFactor;
        this.enforcementPercentage = enforcementPercentage;
        this.minimumHosts = minimumHosts;
        this.requestVolume = requestVolume;
    }

    @Nullable
    public Integer getStdevFactor() {
        return stdevFactor;
    }

    @Nullable
    public Integer getEnforcementPercentage() {
        return enforcementPercentage;
    }

    @Nullable
    public Integer getMinimumHosts() {
        return minimumHosts;
    }

    @Nullable
    public Integer getRequestVolume() {
        return requestVolume;
    }

    @Override
    public String toString() {
        return "SuccessRateEjection{" + "stdevFactor=" + stdevFactor + ", " + "enforcementPercentage="
                + enforcementPercentage + ", " + "minimumHosts=" + minimumHosts + ", " + "requestVolume="
                + requestVolume + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof SuccessRateEjection) {
            SuccessRateEjection that = (SuccessRateEjection) o;
            return (this.stdevFactor == null
                            ? that.getStdevFactor() == null
                            : this.stdevFactor.equals(that.getStdevFactor()))
                    && (this.enforcementPercentage == null
                            ? that.getEnforcementPercentage() == null
                            : this.enforcementPercentage.equals(that.getEnforcementPercentage()))
                    && (this.minimumHosts == null
                            ? that.getMinimumHosts() == null
                            : this.minimumHosts.equals(that.getMinimumHosts()))
                    && (this.requestVolume == null
                            ? that.getRequestVolume() == null
                            : this.requestVolume.equals(that.getRequestVolume()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (stdevFactor == null) ? 0 : stdevFactor.hashCode();
        h$ *= 1000003;
        h$ ^= (enforcementPercentage == null) ? 0 : enforcementPercentage.hashCode();
        h$ *= 1000003;
        h$ ^= (minimumHosts == null) ? 0 : minimumHosts.hashCode();
        h$ *= 1000003;
        h$ ^= (requestVolume == null) ? 0 : requestVolume.hashCode();
        return h$;
    }
}
