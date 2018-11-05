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
}
