package org.apache.dubbo.servicedata.metadata;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author cvictory ON 2018/9/18
 */
public class MethodDescriptor {
    private String name;
    private String[] parameterTypes;
    private String returnType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodDescriptor)) return false;
        MethodDescriptor that = (MethodDescriptor) o;
        return Objects.equals(getName(), that.getName()) &&
                Arrays.equals(getParameterTypes(), that.getParameterTypes()) &&
                Objects.equals(getReturnType(), that.getReturnType());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getName(), getReturnType());
        result = 31 * result + Arrays.hashCode(getParameterTypes());
        return result;
    }
}
