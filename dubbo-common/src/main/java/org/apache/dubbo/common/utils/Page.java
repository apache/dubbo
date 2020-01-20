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

import java.util.List;

/**
 * The model class of pagination
 *
 * @since 2.7.5
 */
public interface Page<T> {

    /**
     * Gets the offset of request
     *
     * @return positive integer
     */
    int getOffset();

    /**
     * Gets the size of request for pagination query
     *
     * @return positive integer
     */
    int getPageSize();

    /**
     * Gets the total amount of elements.
     *
     * @return the total amount of elements
     */
    int getTotalSize();

    /**
     * Get the number of total pages.
     *
     * @return the number of total pages.
     */
    int getTotalPages();

    /**
     * The data of current page
     *
     * @return non-null {@link List}
     */
    List<T> getData();

    /**
     * The size of {@link #getData() data}
     *
     * @return positive integer
     */
    default int getDataSize() {
        return getData().size();
    }

    /**
     * It indicates has next page or not
     *
     * @return if has , return <code>true</code>, or <code>false</code>
     */
    boolean hasNext();

    /**
     * Returns whether the page has data at all.
     *
     * @return
     */
    default boolean hasData() {
        return getDataSize() > 0;
    }
}
