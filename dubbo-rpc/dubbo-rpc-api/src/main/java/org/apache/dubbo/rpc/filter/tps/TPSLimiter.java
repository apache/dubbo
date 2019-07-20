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
package org.apache.dubbo.rpc.filter.tps;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

/**
 * Provide boolean information whether a invocation of a provider service's methods or a particular method
 * is allowed within a last invocation and current invocation.
 * <pre>
 *     e.g. if tps for a method m1 is 5 for a minute then if 6th call is made within the span of 1 minute then 6th
 *     should not be allowed <b>isAllowable</b> will return false.
 * </pre>
 */
public interface TPSLimiter {

    /**
     * judge if the current invocation is allowed by TPS rule
     *
     * @param url        url
     * @param invocation invocation
     * @return true allow the current invocation, otherwise, return false
     */
    boolean isAllowable(URL url, Invocation invocation);

}
