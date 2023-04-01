package org.apache.dubbo.metrics.model;

import org.apache.dubbo.common.utils.Assert;

public class TypeWrapper {
    private final MetricsLevel level;
    private final Object postType;
    private final Object finishType;
    private final Object errorType;

    public TypeWrapper(MetricsLevel level, Object postType, Object finishType, Object errorType) {
        this.level = level;
        this.postType = postType;
        this.finishType = finishType;
        this.errorType = errorType;
    }

    public MetricsLevel getLevel() {
        return level;
    }

    public Object getErrorType() {
        return errorType;
    }

    public boolean isAssignableFrom(Object type) {
        Assert.notNull(type, "Type can not be null");
        return type.equals(postType) || type.equals(finishType) || type.equals(errorType);
    }
}
