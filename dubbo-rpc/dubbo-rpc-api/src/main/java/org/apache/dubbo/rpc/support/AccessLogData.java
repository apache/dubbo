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
package org.apache.dubbo.rpc.support;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AccessLogData is a container for log event data. In internally uses map and store each filed of log as value. It does not generate any
 * dynamic value e.g. time stamp, local jmv machine host address etc. It does not allow any null or empty key.
 */
public final class AccessLogData {
    /**
     * Access log keys
      */
    private enum KEYS {
        VERSION, GROUP, SERVICE, METHOD_NAME, INVOCATION_TIME, TYPES
        ,ARGUMENTS, REMOTE_HOST, REMOTE_PORT, LOCAL_HOST, LOCAL_PORT
    }

    /**
     * This is used to store log data in key val format.
     */
    private Map<KEYS, Object> data;

    /**
     * Default constructor.
     */
    private AccessLogData() {
        data = new HashMap<>();
    }

    /**
     * Get new instance of log data.
     *
     * @return instance of AccessLogData
     */
    public static AccessLogData newLogData() {
        return new AccessLogData();
    }

    /**
     * Sets the access log key
     * @param accessLogKey
     */
    public void setAccessLogKey(String accessLogKey) {

    }
    /**
     * Add version information.
     *
     * @param version
     */
    public void setVersion(String version) {
        set(KEYS.VERSION, version);
    }

    /**
     * Add service name.
     *
     * @param serviceName
     */
    public void setServiceName(String serviceName) {
        set(KEYS.SERVICE, serviceName);
    }

    /**
     * Add group name
     *
     * @param group
     */
    public void setGroup(String group) {
        set(KEYS.GROUP, group);
    }

    /**
     * Set the invocation date. As an argument it accept date string.
     *
     * @param invocationTime
     */
    public void setInvocationTime(Date invocationTime) {
        set(KEYS.INVOCATION_TIME, invocationTime);
    }

    /**
     * Set caller remote host
     *
     * @param remoteHost
     */
    public void setRemoteHost(String remoteHost) {
        set(KEYS.REMOTE_HOST, remoteHost);
    }

    /**
     * Set caller remote port.
     *
     * @param remotePort
     */
    public void setRemotePort(Integer remotePort) {
        set(KEYS.REMOTE_PORT, remotePort);
    }

    /**
     * Set local host
     *
     * @param localHost
     */
    public void setLocalHost(String localHost) {
        set(KEYS.LOCAL_HOST, localHost);
    }

    /**
     * Set local port of exported service
     *
     * @param localPort
     */
    public void setLocalPort(Integer localPort) {
        set(KEYS.LOCAL_PORT, localPort);
    }

    /**
     * Set target method name.
     *
     * @param methodName
     */
    public void setMethodName(String methodName) {
        set(KEYS.METHOD_NAME, methodName);
    }

    /**
     * Set invocation's method's input parameter's types
     *
     * @param types
     */
    public void setTypes(Class[] types) {
        set(KEYS.TYPES, types != null ? Arrays.copyOf(types, types.length) : null);
    }

    /**
     * Sets invocation arguments
     *
     * @param arguments
     */
    public void setArguments(Object[] arguments) {
        set(KEYS.ARGUMENTS, arguments != null ? Arrays.copyOf(arguments, arguments.length) : null);
    }

    /**
     * Return gthe service of access log entry
     * @return
     */
    public String getServiceName() {
        return get(KEYS.SERVICE).toString();
    }


    public String getLogMessage(SimpleDateFormat sdf) {
        StringBuilder sn = new StringBuilder();

        sn.append("[")
                .append(sdf.format(get(KEYS.INVOCATION_TIME)))
                .append("] ")
                .append(get(KEYS.REMOTE_HOST))
                .append(":")
                .append(get(KEYS.REMOTE_PORT))
                .append(" -> ")
                .append(get(KEYS.LOCAL_HOST))
                .append(":")
                .append(get(KEYS.LOCAL_PORT))
                .append(" - ");

        String group = get(KEYS.GROUP) != null ? get(KEYS.GROUP).toString() : "";
        if (StringUtils.isNotEmpty(group.toString())) {
            sn.append(group).append("/");
        }

        sn.append(get(KEYS.SERVICE));

        String version = get(KEYS.VERSION) != null ? get(KEYS.VERSION).toString() : "";
        if (StringUtils.isNotEmpty(version.toString())) {
            sn.append(":").append(version);
        }

        sn.append(" ");
        sn.append(get(KEYS.METHOD_NAME));

        sn.append("(");
        Class<?>[] types = get(KEYS.TYPES) != null? (Class<?>[]) get(KEYS.TYPES) : new Class[0];
        boolean first = true;
        for (Class<?> type : types) {
            if (first) {
                first = false;
            } else {
                sn.append(",");
            }
            sn.append(type.getName());
        }
        sn.append(") ");


        Object[] args = get(KEYS.ARGUMENTS) !=null ? (Object[]) get(KEYS.ARGUMENTS) : null;
        if (args != null && args.length > 0) {
            sn.append(JSON.toJSONString(args));
        }

        return sn.toString();
    }

    /**
     * Return value of key
     *
     * @param key
     * @return
     */
    private Object get(KEYS key) {
        return data.get(key);
    }

    /**
     * Add log key along with his value.
     *
     * @param key   Any not null or non empty string
     * @param value Any object including null.
     */
    private void set(KEYS key, Object value) {
        data.put(key, value);
    }

}
