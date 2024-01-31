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
package org.apache.dubbo.metrics.model.key;

import org.apache.dubbo.common.lang.Nullable;

/**
 *  The overall event set, including the event processing functions in three stages
 */
public class CategoryOverall {

    private final MetricsCat post;
    private MetricsCat finish;
    private MetricsCat error;

    /**
     * @param placeType When placeType is null, it means that placeType is obtained dynamically
     * @param post      Statistics of the number of events, as long as it occurs, it will take effect, so it cannot be null
     */
    public CategoryOverall(
            @Nullable MetricsPlaceValue placeType,
            MetricsCat post,
            @Nullable MetricsCat finish,
            @Nullable MetricsCat error) {
        this.post = post.setPlaceType(placeType);
        if (finish != null) {
            this.finish = finish.setPlaceType(placeType);
        }
        if (error != null) {
            this.error = error.setPlaceType(placeType);
        }
    }

    public MetricsCat getPost() {
        return post;
    }

    public MetricsCat getFinish() {
        return finish;
    }

    public MetricsCat getError() {
        return error;
    }
}
