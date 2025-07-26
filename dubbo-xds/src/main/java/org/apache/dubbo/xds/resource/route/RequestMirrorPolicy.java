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
package org.apache.dubbo.xds.resource.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.common.FractionalPercent;

import java.util.Objects;

/**
 * Configuration for request mirroring.
 */
public final class RequestMirrorPolicy {

    private final String cluster;

    @Nullable
    private final FractionalPercent runtimeFraction;

    @Nullable
    private final Boolean traceSampled;

    public RequestMirrorPolicy(String cluster, @Nullable FractionalPercent runtimeFraction, @Nullable Boolean traceSampled) {
        this.cluster = cluster;
        this.runtimeFraction = runtimeFraction;
        this.traceSampled = traceSampled;
    }

    public String getCluster() {
        return cluster;
    }

    @Nullable
    public FractionalPercent getRuntimeFraction() {
        return runtimeFraction;
    }

    @Nullable
    public Boolean getTraceSampled() {
        return traceSampled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestMirrorPolicy that = (RequestMirrorPolicy) o;
        return Objects.equals(cluster, that.cluster) &&
            Objects.equals(runtimeFraction, that.runtimeFraction) &&
            Objects.equals(traceSampled, that.traceSampled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cluster, runtimeFraction, traceSampled);
    }

    @Override
    public String toString() {
        return "RequestMirrorPolicy{" +
            "cluster='" + cluster + '\'' +
            ", runtimeFraction=" + runtimeFraction +
            ", traceSampled=" + traceSampled +
            '}';
    }
} 