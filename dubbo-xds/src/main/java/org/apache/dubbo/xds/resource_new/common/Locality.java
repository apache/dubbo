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
package org.apache.dubbo.xds.resource_new.common;

public class Locality {

    private String region;

    private String zone;

    private String subZone;

    public Locality(String region, String zone, String subZone) {
        if (region == null) {
            throw new NullPointerException("Null region");
        }
        this.region = region;
        if (zone == null) {
            throw new NullPointerException("Null zone");
        }
        this.zone = zone;
        if (subZone == null) {
            throw new NullPointerException("Null subZone");
        }
        this.subZone = subZone;
    }

    String region() {
        return region;
    }

    String zone() {
        return zone;
    }

    String subZone() {
        return subZone;
    }

    @Override
    public String toString() {
        return "Locality{" + "region=" + region + ", " + "zone=" + zone + ", " + "subZone=" + subZone + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Locality) {
            Locality that = (Locality) o;
            return this.region.equals(that.region())
                    && this.zone.equals(that.zone())
                    && this.subZone.equals(that.subZone());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= region.hashCode();
        h$ *= 1000003;
        h$ ^= zone.hashCode();
        h$ *= 1000003;
        h$ ^= subZone.hashCode();
        return h$;
    }
}
