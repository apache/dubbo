package com.alibaba.dubbo.registry.common.util;

import java.util.Locale;

public class LocaleUtils {
	
	private LocaleUtils() {}

	public static Locale getLocale(String language) {
    	if ("en".equalsIgnoreCase(language)) {
        	return Locale.ENGLISH;
        } else if ("zh".equalsIgnoreCase(language)) {
        	return Locale.SIMPLIFIED_CHINESE;
        } else if ("zh_TW".equalsIgnoreCase(language)) {
        	return Locale.TRADITIONAL_CHINESE;
        }
        return Locale.getDefault();
    }

}
