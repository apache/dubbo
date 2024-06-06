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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.Ordered;

public class OrderedObjectCompareUtils {

    public static int compareByOrder(Object o1, Object o2) {
        boolean o1Ordered = o1 instanceof Ordered;
        boolean o2Ordered = o2 instanceof Ordered;
        if (!o1Ordered) {
            if (!o2Ordered) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (!o2Ordered) {
                return 1;
            } else {
                return Integer.compare(((Ordered) o1).getOrder(), ((Ordered) o2).getOrder());
            }
        }
    }
}
