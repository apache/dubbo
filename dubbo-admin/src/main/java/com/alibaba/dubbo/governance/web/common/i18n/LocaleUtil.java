package com.alibaba.dubbo.governance.web.common.i18n;

import java.util.Locale;

public class LocaleUtil {
    private static ThreadLocal<Locale> userLocale = new ThreadLocal<Locale>();

    public static void cleanLocale() {
        userLocale.remove();
    }

    public static Locale getLocale() {
        return userLocale.get();
    }

    public static void setLocale(Locale locale) {
        userLocale.set(locale);
    }

}
