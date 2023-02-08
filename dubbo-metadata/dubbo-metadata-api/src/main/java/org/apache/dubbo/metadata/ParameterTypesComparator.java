package org.apache.dubbo.metadata;

import java.util.Arrays;

public class ParameterTypesComparator {

    private Class[] parameterTypes;


    public ParameterTypesComparator(Class[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterTypesComparator that = (ParameterTypesComparator) o;
        return Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parameterTypes);
    }


    public static ParameterTypesComparator getInstance(Class[] parameterTypes) {
        return new ParameterTypesComparator(parameterTypes);
    }


}
