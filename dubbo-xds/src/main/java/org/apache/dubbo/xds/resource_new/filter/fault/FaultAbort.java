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
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.xds.resource_new.common.FractionalPercent;

import io.grpc.Status;

final class FaultAbort {

    @Nullable
    private final Status status;

    private final boolean headerAbort;

    private final FractionalPercent percent;

    static FaultAbort forStatus(Status status, FractionalPercent percent) {
        Assert.notNull(status, "status must not be null");
        return FaultAbort.create(status, false, percent);
    }

    static FaultAbort forHeader(FractionalPercent percent) {
        return FaultAbort.create(null, true, percent);
    }

    public static FaultAbort create(@Nullable Status status, boolean headerAbort, FractionalPercent percent) {
        return new FaultAbort(status, headerAbort, percent);
    }

    FaultAbort(@Nullable Status status, boolean headerAbort, FractionalPercent percent) {
        this.status = status;
        this.headerAbort = headerAbort;
        if (percent == null) {
            throw new NullPointerException("Null percent");
        }
        this.percent = percent;
    }

    @Nullable
    public Status getStatus() {
        return status;
    }

    public boolean getHeaderAbort() {
        return headerAbort;
    }

    public FractionalPercent getPercent() {
        return percent;
    }

    @Override
    public String toString() {
        return "FaultAbort{" + "status=" + status + ", " + "headerAbort=" + headerAbort + ", " + "percent=" + percent
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FaultAbort) {
            FaultAbort that = (FaultAbort) o;
            return (this.status == null ? that.getStatus() == null : this.status.equals(that.getStatus()))
                    && this.headerAbort == that.getHeaderAbort()
                    && this.percent.equals(that.getPercent());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (status == null) ? 0 : status.hashCode();
        h$ *= 1000003;
        h$ ^= headerAbort ? 1231 : 1237;
        h$ *= 1000003;
        h$ ^= percent.hashCode();
        return h$;
    }
}
