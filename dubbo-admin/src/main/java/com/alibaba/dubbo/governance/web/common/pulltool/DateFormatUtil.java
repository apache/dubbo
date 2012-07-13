package com.alibaba.dubbo.governance.web.common.pulltool;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * DateFormat Utility
 * 
 * @author guanghui.shigh
 */
public class DateFormatUtil {

    private static final String                               DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final ThreadLocal<Map<String, DateFormat>> tl             = new ThreadLocal<Map<String, DateFormat>>();

    /**
     * According to the specified format, Get a DateFormat
     * 
     * @param format
     * @return
     */
    public static DateFormat getDateFormat(String format) {
        Map<String, DateFormat> map = tl.get();

        if (map == null) {
            map = new HashMap<String, DateFormat>();
            tl.set(map);
        }

        if (StringUtils.isEmpty(format)) {
            format = DEFAULT_FORMAT;
        }

        DateFormat ret = map.get(format);

        if (ret == null) {
            ret = new SimpleDateFormat(format);
            map.put(format, ret);
        }

        return ret;
    }

    /**
     * Get Default DateFormat
     * 
     * @return
     */
    public static DateFormat getDateFormat() {
        return getDateFormat(null);
    }
}
