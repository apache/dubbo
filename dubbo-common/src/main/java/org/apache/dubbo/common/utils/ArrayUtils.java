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

/**
 * Contains some methods to check array.
 */
public final class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * <p>Checks if the array is null or empty. <p/>
     *
     * @param array th array to check
     * @return {@code true} if the array is null or empty.
     */
    public static boolean isEmpty(final Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * <p>Checks if the array is not null or empty. <p/>
     *
     * @param array th array to check
     * @return {@code true} if the array is not null or empty.
     */
    public static boolean isNotEmpty(final Object[] array) {
        return !isEmpty(array);
    }

    public static boolean contains(final String[] array, String valueToFind) {
        return indexOf(array, valueToFind, 0) != -1;
    }

    public static int indexOf(String[] array, String valueToFind, int startIndex) {
        if (isEmpty(array) || valueToFind == null) {
            return -1;
        } else {
            if (startIndex < 0) {
                startIndex = 0;
            }

            for (int i = startIndex; i < array.length; ++i) {
                if (valueToFind.equals(array[i])) {
                    return i;
                }
            }

            return -1;
        }
    }
}
