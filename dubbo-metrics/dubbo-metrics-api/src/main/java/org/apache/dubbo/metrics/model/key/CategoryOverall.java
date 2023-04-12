package org.apache.dubbo.metrics.model.key;

import io.micrometer.common.lang.Nullable;

public class CategoryOverall {

    private final MetricsCat post;
    private MetricsCat finish;
    private MetricsCat error;

    public CategoryOverall(MetricsPlaceType placeType, MetricsCat post, @Nullable MetricsCat finish, @Nullable MetricsCat error) {
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
