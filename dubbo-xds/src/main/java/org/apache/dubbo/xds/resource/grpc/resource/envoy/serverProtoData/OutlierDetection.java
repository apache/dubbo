package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import org.apache.dubbo.common.lang.Nullable;

import com.google.protobuf.util.Durations;

public class OutlierDetection {

    @Nullable
    private final Long intervalNanos;

    @Nullable
    private final Long baseEjectionTimeNanos;

    @Nullable
    private final Long maxEjectionTimeNanos;

    @Nullable
    private final Integer maxEjectionPercent;

    @Nullable
    private final SuccessRateEjection successRateEjection;

    @Nullable
    private final FailurePercentageEjection failurePercentageEjection;

    static OutlierDetection create(
            @Nullable Long intervalNanos,
            @Nullable Long baseEjectionTimeNanos,
            @Nullable Long maxEjectionTimeNanos,
            @Nullable Integer maxEjectionPercentage,
            @Nullable SuccessRateEjection successRateEjection,
            @Nullable FailurePercentageEjection failurePercentageEjection) {
        return new OutlierDetection(intervalNanos, baseEjectionTimeNanos, maxEjectionTimeNanos, maxEjectionPercentage
                , successRateEjection, failurePercentageEjection);
    }

    public static OutlierDetection fromEnvoyOutlierDetection(
            io.envoyproxy.envoy.config.cluster.v3.OutlierDetection envoyOutlierDetection) {

        Long intervalNanos = envoyOutlierDetection.hasInterval() ?
                Durations.toNanos(envoyOutlierDetection.getInterval()) : null;
        Long baseEjectionTimeNanos = envoyOutlierDetection.hasBaseEjectionTime() ?
                Durations.toNanos(envoyOutlierDetection.getBaseEjectionTime()) : null;
        Long maxEjectionTimeNanos = envoyOutlierDetection.hasMaxEjectionTime() ?
                Durations.toNanos(envoyOutlierDetection.getMaxEjectionTime()) : null;
        Integer maxEjectionPercentage = envoyOutlierDetection.hasMaxEjectionPercent() ?
                envoyOutlierDetection.getMaxEjectionPercent()
                .getValue() : null;

        SuccessRateEjection successRateEjection;
        // If success rate enforcement has been turned completely off, don't configure this ejection.
        if (envoyOutlierDetection.hasEnforcingSuccessRate() && envoyOutlierDetection.getEnforcingSuccessRate()
                .getValue() == 0) {
            successRateEjection = null;
        } else {
            Integer stdevFactor = envoyOutlierDetection.hasSuccessRateStdevFactor() ?
                    envoyOutlierDetection.getSuccessRateStdevFactor()
                    .getValue() : null;
            Integer enforcementPercentage = envoyOutlierDetection.hasEnforcingSuccessRate() ?
                    envoyOutlierDetection.getEnforcingSuccessRate()
                    .getValue() : null;
            Integer minimumHosts = envoyOutlierDetection.hasSuccessRateMinimumHosts() ?
                    envoyOutlierDetection.getSuccessRateMinimumHosts()
                    .getValue() : null;
            Integer requestVolume = envoyOutlierDetection.hasSuccessRateRequestVolume() ?
                    envoyOutlierDetection.getSuccessRateMinimumHosts()
                    .getValue() : null;

            successRateEjection = SuccessRateEjection.create(stdevFactor, enforcementPercentage, minimumHosts,
                    requestVolume);
        }

        FailurePercentageEjection failurePercentageEjection;
        if (envoyOutlierDetection.hasEnforcingFailurePercentage() &&
                envoyOutlierDetection.getEnforcingFailurePercentage()
                        .getValue() == 0) {
            failurePercentageEjection = null;
        } else {
            Integer threshold = envoyOutlierDetection.hasFailurePercentageThreshold() ?
                    envoyOutlierDetection.getFailurePercentageThreshold()
                    .getValue() : null;
            Integer enforcementPercentage = envoyOutlierDetection.hasEnforcingFailurePercentage() ?
                    envoyOutlierDetection.getEnforcingFailurePercentage()
                    .getValue() : null;
            Integer minimumHosts = envoyOutlierDetection.hasFailurePercentageMinimumHosts() ?
                    envoyOutlierDetection.getFailurePercentageMinimumHosts()
                    .getValue() : null;
            Integer requestVolume = envoyOutlierDetection.hasFailurePercentageRequestVolume() ?
                    envoyOutlierDetection.getFailurePercentageRequestVolume()
                    .getValue() : null;

            failurePercentageEjection = FailurePercentageEjection.create(threshold, enforcementPercentage,
                    minimumHosts, requestVolume);
        }

        return create(intervalNanos, baseEjectionTimeNanos, maxEjectionTimeNanos, maxEjectionPercentage,
                successRateEjection, failurePercentageEjection);
    }

    public OutlierDetection(
            @Nullable Long intervalNanos,
            @Nullable Long baseEjectionTimeNanos,
            @Nullable Long maxEjectionTimeNanos,
            @Nullable Integer maxEjectionPercent,
            @Nullable SuccessRateEjection successRateEjection,
            @Nullable FailurePercentageEjection failurePercentageEjection) {
        this.intervalNanos = intervalNanos;
        this.baseEjectionTimeNanos = baseEjectionTimeNanos;
        this.maxEjectionTimeNanos = maxEjectionTimeNanos;
        this.maxEjectionPercent = maxEjectionPercent;
        this.successRateEjection = successRateEjection;
        this.failurePercentageEjection = failurePercentageEjection;
    }

    @Nullable
    Long intervalNanos() {
        return intervalNanos;
    }

    @Nullable
    Long baseEjectionTimeNanos() {
        return baseEjectionTimeNanos;
    }

    @Nullable
    Long maxEjectionTimeNanos() {
        return maxEjectionTimeNanos;
    }

    @Nullable
    Integer maxEjectionPercent() {
        return maxEjectionPercent;
    }

    @Nullable
    SuccessRateEjection successRateEjection() {
        return successRateEjection;
    }

    @Nullable
    FailurePercentageEjection failurePercentageEjection() {
        return failurePercentageEjection;
    }

    @Override
    public String toString() {
        return "OutlierDetection{" + "intervalNanos=" + intervalNanos + ", " + "baseEjectionTimeNanos="
                + baseEjectionTimeNanos + ", " + "maxEjectionTimeNanos=" + maxEjectionTimeNanos + ", "
                + "maxEjectionPercent=" + maxEjectionPercent + ", " + "successRateEjection=" + successRateEjection
                + ", " + "failurePercentageEjection=" + failurePercentageEjection + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof OutlierDetection) {
            OutlierDetection that = (OutlierDetection) o;
            return (this.intervalNanos == null ?
                    that.intervalNanos() == null : this.intervalNanos.equals(that.intervalNanos())) && (
                    this.baseEjectionTimeNanos == null ? that.baseEjectionTimeNanos()
                            == null : this.baseEjectionTimeNanos.equals(that.baseEjectionTimeNanos())) && (
                    this.maxEjectionTimeNanos == null ? that.maxEjectionTimeNanos()
                            == null : this.maxEjectionTimeNanos.equals(that.maxEjectionTimeNanos())) && (
                    this.maxEjectionPercent == null ? that.maxEjectionPercent()
                            == null : this.maxEjectionPercent.equals(that.maxEjectionPercent())) && (
                    this.successRateEjection == null ? that.successRateEjection()
                            == null : this.successRateEjection.equals(that.successRateEjection())) && (
                    this.failurePercentageEjection == null ? that.failurePercentageEjection()
                            == null : this.failurePercentageEjection.equals(that.failurePercentageEjection()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (intervalNanos == null) ? 0 : intervalNanos.hashCode();
        h$ *= 1000003;
        h$ ^= (baseEjectionTimeNanos == null) ? 0 : baseEjectionTimeNanos.hashCode();
        h$ *= 1000003;
        h$ ^= (maxEjectionTimeNanos == null) ? 0 : maxEjectionTimeNanos.hashCode();
        h$ *= 1000003;
        h$ ^= (maxEjectionPercent == null) ? 0 : maxEjectionPercent.hashCode();
        h$ *= 1000003;
        h$ ^= (successRateEjection == null) ? 0 : successRateEjection.hashCode();
        h$ *= 1000003;
        h$ ^= (failurePercentageEjection == null) ? 0 : failurePercentageEjection.hashCode();
        return h$;
    }

}
