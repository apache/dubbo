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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match;


public class DoubleRangeMatch {
    private Double start;
    private Double end;

    public Double getStart() {
        return start;
    }

    public void setStart(Double start) {
        this.start = start;
    }

    public Double getEnd() {
        return end;
    }

    public void setEnd(Double end) {
        this.end = end;
    }


    public static boolean isMatch(DoubleRangeMatch doubleRangeMatch, Double input) {
        if (doubleRangeMatch.getStart() != null && doubleRangeMatch.getEnd() != null) {
            return input.compareTo(doubleRangeMatch.getStart()) >= 0 && input.compareTo(doubleRangeMatch.getEnd()) < 0;
        } else if (doubleRangeMatch.getStart() != null) {
            return input.compareTo(doubleRangeMatch.getStart()) >= 0;
        } else if (doubleRangeMatch.getEnd() != null) {
            return input.compareTo(doubleRangeMatch.getEnd()) < 0;
        } else {
            return false;
        }
    }
}
