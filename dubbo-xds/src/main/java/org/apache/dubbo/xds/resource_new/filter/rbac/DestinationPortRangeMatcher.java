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
package org.apache.dubbo.xds.resource_new.filter.rbac;

final class DestinationPortRangeMatcher implements Matcher {

    private final int start;

    private final int end;

    /**
     * Start of the range is inclusive. End of the range is exclusive.
     */
    public static DestinationPortRangeMatcher create(int start, int end) {
        return new DestinationPortRangeMatcher(start, end);
    }

    @Override
    public boolean matches(Object args) {
        return true;
    }

    DestinationPortRangeMatcher(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    @Override
    public String toString() {
        return "DestinationPortRangeMatcher{" + "start=" + start + ", " + "end=" + end + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DestinationPortRangeMatcher) {
            DestinationPortRangeMatcher that = (DestinationPortRangeMatcher) o;
            return this.start == that.start() && this.end == that.end();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= start;
        h$ *= 1000003;
        h$ ^= end;
        return h$;
    }
}
