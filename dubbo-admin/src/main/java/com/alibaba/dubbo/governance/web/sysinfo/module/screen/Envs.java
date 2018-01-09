/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
