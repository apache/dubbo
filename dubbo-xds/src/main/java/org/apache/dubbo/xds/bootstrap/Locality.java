/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.bootstrap;

import com.google.auto.value.AutoValue;
import io.grpc.Internal;

import java.util.Objects;

/** Represents a network locality. */
@Internal
public class Locality {
    private final String region;
    private final String zone;
    private final String subZone;

    public Locality(String region, String zone, String subZone) {
        this.region = region;
        this.zone = zone;
        this.subZone = subZone;
    }

    public String getRegion() {
        return region;
    }

    public String getZone() {
        return zone;
    }

    public String getSubZone() {
        return subZone;
    }

    public static Locality create(String region, String zone, String subZone) {
        return new Locality(region, zone, subZone);
    }

    @Override
    public String toString() {
        return "Locality{" +
                "region='" + region + '\'' +
                ", zone='" + zone + '\'' +
                ", subZone='" + subZone + '\'' +
                '}';
    }
}
