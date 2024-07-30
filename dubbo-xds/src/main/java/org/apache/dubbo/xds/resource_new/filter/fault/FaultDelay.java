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

    private static FaultDelay create(
            @Nullable Long delayNanos, boolean headerDelay, FractionalPercent percent) {
        return new FaultDelay(delayNanos, headerDelay, percent);
    }

    FaultDelay(
            @Nullable Long delayNanos, boolean headerDelay, FractionalPercent percent) {
        this.delayNanos = delayNanos;
        this.headerDelay = headerDelay;
        if (percent == null) {
            throw new NullPointerException("Null percent");
        }
        this.percent = percent;
    }

    @Nullable
    Long delayNanos() {
        return delayNanos;
    }

    boolean headerDelay() {
        return headerDelay;
    }

    FractionalPercent percent() {
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
            return (this.delayNanos == null ? that.delayNanos() == null : this.delayNanos.equals(that.delayNanos()))
                    && this.headerDelay == that.headerDelay() && this.percent.equals(that.percent());
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
