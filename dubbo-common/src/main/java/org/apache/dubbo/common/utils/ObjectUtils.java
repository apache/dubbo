package org.apache.dubbo.common.utils;

/**
 * User: aini
 * Date: 2023/1/19 15:38
 */
public final class ObjectUtils {


    /**
     * Convert from variable arguments to array
     *
     * @param values variable arguments
     * @param <T>    The class
     * @return array
     */
    public static <T> T[] of(T... values) {
        return values;
    }

    private ObjectUtils() {

    }
}
