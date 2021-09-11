package org.apache.dubbo.common.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Map tools
 */
public class MapUtils {

    /**
     * switch Map<String, Object> to Map<String, String>
     *
     * If the value of the original Map is not of type String, then toString() of value will be called
     *
     * @param originMap
     * @return
     */
    public static Map<String, String> objectToStringMap(Map<String, Object> originMap) {
        Map<String, String> newStrMap = new HashMap<>();

        if (originMap == null) {
            return newStrMap;
        }

        for (Map.Entry<String, Object> entry : originMap.entrySet()) {
            String stringValue = convertToString(entry.getValue());
            if (stringValue != null) {
                newStrMap.put(entry.getKey(), stringValue);
            }
        }

        return newStrMap;
    }

    /**
     * use {@link Object#toString()} switch Obj to String
     * @param obj
     * @return
     */
    private static String convertToString(Object obj) {
        if (obj == null) {
            return null;
        } else {
            return obj.toString();
        }
    }
}
