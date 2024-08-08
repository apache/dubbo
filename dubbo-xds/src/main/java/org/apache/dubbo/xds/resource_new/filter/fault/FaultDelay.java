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
package org.apache.dubbo.xds.resource_new.filter.fault;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource_new.common.FractionalPercent;

final class FaultDelay {

    @Nullable
    private final Long delayNanos;

    private final boolean headerDelay;

    private final FractionalPercent percent;

    static FaultDelay forFixedDelay(long delayNanos, FractionalPercent percent) {
        return FaultDelay.create(delayNanos, false, percent);
    }

    static FaultDelay forHeader(FractionalPercent percentage) {
        return FaultDelay.create(null, true, percentage);
    }

    private static FaultDelay create(@Nullable Long delayNanos, boolean headerDelay, FractionalPercent percent) {
        return new FaultDelay(delayNanos, headerDelay, percent);
    }

    FaultDelay(@Nullable Long delayNanos, boolean headerDelay, FractionalPercent percent) {
        this.delayNanos = delayNanos;
        this.headerDelay = headerDelay;
        if (percent == null) {
            throw new NullPointerException("Null percent");
        }
        this.percent = percent;
    }

    @Nullable
    public Long getDelayNanos() {
        return delayNanos;
    }

    public boolean getHeaderDelay() {
        return headerDelay;
    }

    public FractionalPercent getPercent() {
        return percent;
    }

    @Override
    public String toString() {
        return "FaultDelay{" + "delayNanos=" + delayNanos + ", " + "headerDelay=" + headerDelay + ", " + "percent="
                + percent + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FaultDelay) {
            FaultDelay that = (FaultDelay) o;
            return (this.delayNanos == null
                            ? that.getDelayNanos() == null
                            : this.delayNanos.equals(that.getDelayNanos()))
                    && this.headerDelay == that.getHeaderDelay()
                    && this.percent.equals(that.getPercent());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (delayNanos == null) ? 0 : delayNanos.hashCode();
        h$ *= 1000003;
        h$ ^= headerDelay ? 1231 : 1237;
        h$ *= 1000003;
        h$ ^= percent.hashCode();
        return h$;
    }
}
