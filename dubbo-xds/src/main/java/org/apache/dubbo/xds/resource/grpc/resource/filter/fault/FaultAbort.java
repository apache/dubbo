package org.apache.dubbo.xds.resource.grpc.resource.filter.fault;

import org.apache.dubbo.common.lang.Nullable;

import io.grpc.Status;

import static com.google.common.base.Preconditions.checkNotNull;

final class FaultAbort {

    @Nullable
    private final Status status;

    private final boolean headerAbort;

    private final FractionalPercent percent;

    static FaultAbort forStatus(
            Status status, FractionalPercent percent) {
        checkNotNull(status, "status");
        return FaultAbort.create(status, false, percent);
    }

    static FaultAbort forHeader(FractionalPercent percent) {
        return FaultAbort.create(null, true, percent);
    }

    public static FaultAbort create(
            @Nullable Status status, boolean headerAbort, FractionalPercent percent) {
        return new FaultAbort(status, headerAbort, percent);
    }

    FaultAbort(
            @Nullable Status status, boolean headerAbort, FractionalPercent percent) {
        this.status = status;
        this.headerAbort = headerAbort;
        if (percent == null) {
            throw new NullPointerException("Null percent");
        }
        this.percent = percent;
    }

    @Nullable
    Status status() {
        return status;
    }

    boolean headerAbort() {
        return headerAbort;
    }

    FractionalPercent percent() {
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
            return (this.status == null ? that.status() == null : this.status.equals(that.status()))
                    && this.headerAbort == that.headerAbort() && this.percent.equals(that.percent());
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
