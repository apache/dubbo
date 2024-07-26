package org.apache.dubbo.xds.resource.grpc.resource.filter.fault;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.resource.filter.FilterConfig;

final class FaultConfig implements FilterConfig {

    @Nullable
    private final FaultDelay faultDelay;

    @Nullable
    private final FaultAbort faultAbort;

    @Nullable
    private final Integer maxActiveFaults;

    static FaultConfig create(
            @Nullable FaultDelay faultDelay, @Nullable FaultAbort faultAbort,
            @Nullable Integer maxActiveFaults) {
        return new FaultConfig(faultDelay, faultAbort, maxActiveFaults);
    }

    FaultConfig(
            @Nullable FaultDelay faultDelay,
            @Nullable FaultAbort faultAbort,
            @Nullable Integer maxActiveFaults) {
        this.faultDelay = faultDelay;
        this.faultAbort = faultAbort;
        this.maxActiveFaults = maxActiveFaults;
    }

    @Override
    public final String typeUrl() {
        return FaultFilter.TYPE_URL;
    }

    @Nullable
    FaultDelay faultDelay() {
        return faultDelay;
    }

    @Nullable
    FaultAbort faultAbort() {
        return faultAbort;
    }

    @Nullable
    Integer maxActiveFaults() {
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
            return (this.faultDelay == null ? that.faultDelay() == null : this.faultDelay.equals(that.faultDelay()))
                    && (this.faultAbort == null ? that.faultAbort() == null : this.faultAbort.equals(that.faultAbort()))
                    && (
                    this.maxActiveFaults == null ?
                            that.maxActiveFaults() == null : this.maxActiveFaults.equals(that.maxActiveFaults()));
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
