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

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * The Comparator class for {@link Method}, the comparison rule :
 * <ol>
 *     <li>Comparing to two {@link Method#getName() method names} {@link String#compareTo(String) lexicographically}.
 *     If equals, go to step 2</li>
 *     <li>Comparing to the count of two method parameters. If equals, go to step 3</li>
 *     <li>Comparing to the type names of methods parameter {@link String#compareTo(String) lexicographically}</li>
 * </ol>
 *
 * @since 2.7.6
 */
public class MethodComparator implements Comparator<Method> {

    public final static MethodComparator INSTANCE = new MethodComparator();

    private MethodComparator() {
    }

    @Override
    public int compare(Method m1, Method m2) {

        if (m1.equals(m2)) {
            return 0;
        }

        // Step 1
        String n1 = m1.getName();
        String n2 = m2.getName();
        int value = n1.compareTo(n2);

        if (value == 0) { // Step 2

            Class[] types1 = m1.getParameterTypes();
            Class[] types2 = m2.getParameterTypes();

            value = types1.length - types2.length;

            if (value == 0) { // Step 3
                for (int i = 0; i < types1.length; i++) {
                    value = types1[i].getName().compareTo(types2[i].getName());
                    if (value != 0) {
                        break;
                    }
                }
            }
        }

        return Integer.compare(value, 0);
    }
}
