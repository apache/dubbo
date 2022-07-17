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
package org.apache.dubbo.metrices;

import org.apache.dubbo.quantile.Quantile;

public class MethodMetrics {
    private Integer qps;
    private Quantile rt;
    private Integer succeed;
    private Integer failed;
    private Integer processing;

    public void setQps(Integer qps) {
        this.qps = qps;
    }

    public void setRt(Quantile rt) {
        this.rt = rt;
    }

    public void setSucceed(Integer succeed) {
        this.succeed = succeed;
    }

    public void setFailed(Integer failed) {
        this.failed = failed;
    }

    public void setProcessing(Integer processing) {
        this.processing = processing;
    }

}
