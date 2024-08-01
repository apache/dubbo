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

public final class Range {

    private final long start;

    private final long end;

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    @Override
    public String toString() {
        return "Range{" + "start=" + start + ", " + "end=" + end + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Range) {
            Range that = (Range) o;
            return this.start == that.start() && this.end == that.end();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (int) ((start >>> 32) ^ start);
        h$ *= 1000003;
        h$ ^= (int) ((end >>> 32) ^ end);
        return h$;
    }
}
