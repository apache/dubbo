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
package org.apache.dubbo.quantile;

public class Quantile {
    private Double p99;
    private Double p95;
    private Double max;
    private Double avg;
    private Double min;
    private Double last;

    public void setP99(Double p99) {
        this.p99 = p99;
    }

    public void setP95(Double p95) {
        this.p95 = p95;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public void setLast(Double last) {
        this.last = last;
    }

}
