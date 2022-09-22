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

package org.apache.dubbo.errorcode.config;

import org.apache.dubbo.errorcode.reporter.Reporter;
import org.apache.dubbo.errorcode.util.FileUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration of Error Code Inspector.
 */
public class ErrorCodeInspectorConfig {

    private ErrorCodeInspectorConfig() {
        throw new UnsupportedOperationException("No instance of ErrorCodeInspectorConfig for you! ");
    }

    public static final boolean REPORT_AS_ERROR =
            Boolean.getBoolean("dubbo.eci.report-as-error") ||
            Boolean.parseBoolean(System.getenv("dubbo.eci.report-as-error"));

    public static final List<Reporter> REPORTERS;

    public static final List<String> EXCLUSIONS = FileUtils.loadConfigurationFileInResources("exclusions.cfg");

    static {
        List<String> classNames = FileUtils.loadConfigurationFileInResources("reporter-classes.cfg");

        List<Reporter> tempReporters = new ArrayList<>();

        for (String clsName : classNames) {
            try {
                Class<? extends Reporter> cls = (Class<? extends Reporter>) Class.forName(clsName);
                Reporter r = cls.getConstructor().newInstance();

                tempReporters.add(r);

            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                     IllegalAccessException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        REPORTERS = Collections.unmodifiableList(tempReporters);
    }
}
