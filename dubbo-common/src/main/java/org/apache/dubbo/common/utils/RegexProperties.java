package org.apache.dubbo.common.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Regex matching of keys is supported.
 */
public class RegexProperties extends Properties {

    @Override
    public String getProperty(String key) {
        String value = super.getProperty(key);
        if(value != null) {
            return value;
        }

        List<String> sortedKeyList = keySet().stream().map(k -> (String) k)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        String keyPattern = sortedKeyList
                .stream().filter(k -> Pattern.matches(k, key)).findFirst().orElse(null);

        return keyPattern == null ? null : super.getProperty(keyPattern);
    }
}
