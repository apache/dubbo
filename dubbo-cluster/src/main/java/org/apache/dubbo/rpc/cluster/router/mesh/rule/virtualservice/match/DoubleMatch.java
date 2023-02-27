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


public class DoubleMatch {
    private Double exact;
    private DoubleRangeMatch range;
    private Double mod;

    public Double getExact() {
        return exact;
    }

    public void setExact(Double exact) {
        this.exact = exact;
    }

    public DoubleRangeMatch getRange() {
        return range;
    }

    public void setRange(DoubleRangeMatch range) {
        this.range = range;
    }

    public Double getMod() {
        return mod;
    }

    public void setMod(Double mod) {
        this.mod = mod;
    }


    public boolean isMatch(Double input) {
        if (exact != null && mod == null) {
            return input.equals(exact);
        } else if (range != null) {
            return range.isMatch(input);
        } else if (exact != null) {
            Double result = input % mod;
            return result.equals(exact);
        }

        return false;
    }
}
