/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * <p>
 * File Created at Sep 13, 2011
 * $Id: Envs.java 185206 2012-07-09 03:06:37Z tony.chenl $
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.sysinfo.module.screen;

import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ding.lid
 */
public class Envs extends Restful {

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    public void index(Map<String, Object> context) throws Exception {
        Map<String, String> properties = new TreeMap<String, String>();
        StringBuilder msg = new StringBuilder();
        msg.append("Version: ");
        msg.append(Version.getVersion(Envs.class, "2.2.0"));
        properties.put("Registry", msg.toString());
        String address = NetUtils.getLocalHost();
        properties.put("Host", NetUtils.getHostName(address) + "/" + address);
        properties.put("Java", System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version"));
        properties.put("OS", System.getProperty("os.name") + " "
                + System.getProperty("os.version"));
        properties.put("CPU", System.getProperty("os.arch", "") + ", "
                + String.valueOf(Runtime.getRuntime().availableProcessors()) + " cores");
        properties.put("Locale", Locale.getDefault().toString() + "/"
                + System.getProperty("file.encoding"));
        properties.put("Uptime", formatUptime(ManagementFactory.getRuntimeMXBean().getUptime())
                + " From " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime()))
                + " To " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(new Date()));
        context.put("properties", properties);
    }

    private String formatUptime(long uptime) {
        StringBuilder buf = new StringBuilder();
        if (uptime > DAY) {
            long days = (uptime - uptime % DAY) / DAY;
            buf.append(days);
            buf.append(" Days");
            uptime = uptime % DAY;
        }
        if (uptime > HOUR) {
            long hours = (uptime - uptime % HOUR) / HOUR;
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(hours);
            buf.append(" Hours");
            uptime = uptime % HOUR;
        }
        if (uptime > MINUTE) {
            long minutes = (uptime - uptime % MINUTE) / MINUTE;
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(minutes);
            buf.append(" Minutes");
            uptime = uptime % MINUTE;
        }
        if (uptime > SECOND) {
            long seconds = (uptime - uptime % SECOND) / SECOND;
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(seconds);
            buf.append(" Seconds");
            uptime = uptime % SECOND;
        }
        if (uptime > 0) {
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(uptime);
            buf.append(" Milliseconds");
        }
        return buf.toString();
    }


}
