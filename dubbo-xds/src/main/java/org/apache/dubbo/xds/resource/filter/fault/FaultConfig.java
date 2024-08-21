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
package org.apache.dubbo.xds.resource.filter.fault;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.filter.FilterConfig;

final class FaultConfig implements FilterConfig {

    @Nullable
    private final FaultDelay faultDelay;

    @Nullable
    private final FaultAbort faultAbort;

    @Nullable
    private final Integer maxActiveFaults;

    static FaultConfig create(
            @Nullable FaultDelay faultDelay, @Nullable FaultAbort faultAbort, @Nullable Integer maxActiveFaults) {
        return new FaultConfig(faultDelay, faultAbort, maxActiveFaults);
    }

    FaultConfig(@Nullable FaultDelay faultDelay, @Nullable FaultAbort faultAbort, @Nullable Integer maxActiveFaults) {
        this.faultDelay = faultDelay;
        this.faultAbort = faultAbort;
        this.maxActiveFaults = maxActiveFaults;
    }

    @Override
    public final String typeUrl() {
        return FaultFilter.TYPE_URL;
    }

    @Nullable
    public FaultDelay getFaultDelay() {
        return faultDelay;
    }

    @Nullable
    public FaultAbort getFaultAbort() {
        return faultAbort;
    }

    @Nullable
    public Integer getMaxActiveFaults() {
        return maxActiveFaults;
    }

    @Override
    public String toString() {
        return "FaultConfig{" + "faultDelay=" + faultDelay + ", " + "faultAbort=" + faultAbort + ", "
                + "maxActiveFaults=" + maxActiveFaults + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FaultConfig) {
            FaultConfig that = (FaultConfig) o;
            return (this.faultDelay == null
                            ? that.getFaultDelay() == null
                            : this.faultDelay.equals(that.getFaultDelay()))
                    && (this.faultAbort == null
                            ? that.getFaultAbort() == null
                            : this.faultAbort.equals(that.getFaultAbort()))
                    && (this.maxActiveFaults == null
                            ? that.getMaxActiveFaults() == null
                            : this.maxActiveFaults.equals(that.getMaxActiveFaults()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (faultDelay == null) ? 0 : faultDelay.hashCode();
        h$ *= 1000003;
        h$ ^= (faultAbort == null) ? 0 : faultAbort.hashCode();
        h$ *= 1000003;
        h$ ^= (maxActiveFaults == null) ? 0 : maxActiveFaults.hashCode();
        return h$;
    }
}
