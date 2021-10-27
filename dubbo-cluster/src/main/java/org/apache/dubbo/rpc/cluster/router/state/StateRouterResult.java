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
package org.apache.dubbo.rpc.cluster.router.state;

public class StateRouterResult<T> {
    private final boolean needContinueRoute;
    private final BitList<T> result;
    private final String message;

    public StateRouterResult(BitList<T> result) {
        this.needContinueRoute = true;
        this.result = result;
        this.message = null;
    }

    public StateRouterResult(BitList<T> result, String message) {
        this.needContinueRoute = true;
        this.result = result;
        this.message = message;
    }

    public StateRouterResult(boolean needContinueRoute, BitList<T> result, String message) {
        this.needContinueRoute = needContinueRoute;
        this.result = result;
        this.message = message;
    }

    public boolean isNeedContinueRoute() {
        return needContinueRoute;
    }

    public BitList<T> getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }
}
