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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcContext;

import com.alibaba.fastjson.JSON;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AccessLogData is a container for log event data. In internally uses map and store each filed of log as value. It
 * does not generate any dynamic value e.g. time stamp, local jmv machine host address etc. It does not allow any null
 * or empty key.
 *
 * Note: since its date formatter is a singleton, make sure to run it in single thread only.
 */
public final class AccessLogData {

    private static final String MESSAGE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateFormat MESSAGE_DATE_FORMATTER = new SimpleDateFormat(MESSAGE_DATE_FORMAT);

    private static final String VERSION = "version";
    private static final String GROUP = "group";
    private static final String SERVICE = "service";
    private static final String METHOD_NAME = "method-name";
    private static final String INVOCATION_TIME = "invocation-time";
    private static final String TYPES = "types";
    private static final String ARGUMENTS = "arguments";
    private static final String REMOTE_HOST = "remote-host";
    private static final String REMOTE_PORT = "remote-port";
    private static final String LOCAL_HOST = "localhost";
    private static final String LOCAL_PORT = "local-port";

    /**
     * This is used to store log data in key val format.
     */
    private Map<String, Object> data;

    /**
     * Default constructor.
     */
    private AccessLogData() {
        RpcContext context = RpcContext.getContext();
        data = new HashMap<>();
        setLocalHost(context.getLocalHost());
        setLocalPort(context.getLocalPort());
        setRemoteHost(context.getRemoteHost());
        setRemotePort(context.getRemotePort());
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
     * Add version information.
     *
     * @param version
     */
    public void setVersion(String version) {
        set(VERSION, version);
    }

    /**
     * Add service name.
     *
     * @param serviceName
     */
    public void setServiceName(String serviceName) {
        set(SERVICE, serviceName);
    }

    /**
     * Add group name
     *
     * @param group
     */
    public void setGroup(String group) {
        set(GROUP, group);
    }

    /**
     * Set the invocation date. As an argument it accept date string.
     *
     * @param invocationTime
     */
    public void setInvocationTime(Date invocationTime) {
        set(INVOCATION_TIME, invocationTime);
    }

    /**
     * Set caller remote host
     *
     * @param remoteHost
     */
    private void setRemoteHost(String remoteHost) {
        set(REMOTE_HOST, remoteHost);
    }

    /**
     * Set caller remote port.
     *
     * @param remotePort
     */
    private void setRemotePort(Integer remotePort) {
        set(REMOTE_PORT, remotePort);
    }

    /**
     * Set local host
     *
     * @param localHost
     */
    private void setLocalHost(String localHost) {
        set(LOCAL_HOST, localHost);
    }

    /**
     * Set local port of exported service
     *
     * @param localPort
     */
    private void setLocalPort(Integer localPort) {
        set(LOCAL_PORT, localPort);
    }

    /**
     * Set target method name.
     *
     * @param methodName
     */
    public void setMethodName(String methodName) {
        set(METHOD_NAME, methodName);
    }

    /**
     * Set invocation's method's input parameter's types
     *
     * @param types
     */
    public void setTypes(Class[] types) {
        set(TYPES, types != null ? Arrays.copyOf(types, types.length) : null);
    }

    /**
     * Sets invocation arguments
     *
     * @param arguments
     */
    public void setArguments(Object[] arguments) {
        set(ARGUMENTS, arguments != null ? Arrays.copyOf(arguments, arguments.length) : null);
    }

    /**
     * Return gthe service of access log entry
     *
     * @return
     */
    public String getServiceName() {
        return get(SERVICE).toString();
    }


    public String getLogMessage() {
        StringBuilder sn = new StringBuilder();

        sn.append("[")
                .append(MESSAGE_DATE_FORMATTER.format(getInvocationTime()))
                .append("] ")
                .append(get(REMOTE_HOST))
                .append(":")
                .append(get(REMOTE_PORT))
                .append(" -> ")
                .append(get(LOCAL_HOST))
                .append(":")
                .append(get(LOCAL_PORT))
                .append(" - ");

        String group = get(GROUP) != null ? get(GROUP).toString() : "";
        if (StringUtils.isNotEmpty(group.toString())) {
            sn.append(group).append("/");
        }

        sn.append(get(SERVICE));

        String version = get(VERSION) != null ? get(VERSION).toString() : "";
        if (StringUtils.isNotEmpty(version.toString())) {
            sn.append(":").append(version);
        }

        sn.append(" ");
        sn.append(get(METHOD_NAME));

        sn.append("(");
        Class<?>[] types = get(TYPES) != null ? (Class<?>[]) get(TYPES) : new Class[0];
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


        Object[] args = get(ARGUMENTS) != null ? (Object[]) get(ARGUMENTS) : null;
        if (args != null && args.length > 0) {
            sn.append(JSON.toJSONString(args));
        }

        return sn.toString();
    }

    private Date getInvocationTime() {
        return (Date)get(INVOCATION_TIME);
    }
    /**
     * Return value of key
     *
     * @param key
     * @return
     */
    private Object get(String key) {
        return data.get(key);
    }

    /**
     * Add log key along with his value.
     *
     * @param key   Any not null or non empty string
     * @param value Any object including null.
     */
    private void set(String key, Object value) {
        data.put(key, value);
    }

}
