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

import java.io.Serializable;
import java.util.List;

/**
 * The default implementation of {@link Page}
 *
 * @since 2.7.5
 */
public class DefaultPage<T> implements Page<T>, Serializable {

    private static final long serialVersionUID = 1099331838954070419L;

    private final int requestOffset;

    private final int pageSize;

    private final int totalSize;

    private final List<T> data;

    private final int totalPages;

    private final boolean hasNext;

    public DefaultPage(int requestOffset, int pageSize, List<T> data, int totalSize) {
        this.requestOffset = requestOffset;
        this.pageSize = pageSize;
        this.data = data;
        this.totalSize = totalSize;
        int remain = totalSize % pageSize;
        this.totalPages = remain > 0 ? (totalSize / pageSize) + 1 : totalSize / pageSize;
        this.hasNext = totalSize - requestOffset - pageSize > 0;
    }

    @Override
    public int getOffset() {
        return requestOffset;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public int getTotalSize() {
        return totalSize;
    }

    @Override
    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public List<T> getData() {
        return data;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }
}
