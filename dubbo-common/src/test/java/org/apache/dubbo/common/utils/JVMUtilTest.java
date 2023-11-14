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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_JSTACK_MAXLINE;
import static org.apache.dubbo.common.constants.CommonConstants.OS_NAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.OS_WIN_PREFIX;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_CREATE_DUMP;
import static org.apache.dubbo.common.utils.JVMUtil.jstack;

class JVMUtilTest {

    protected static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JVMUtilTest.class);

    @Test
    void testPrintStackTrace() {
        test(10);
    }

    @Test
    void testPrintStackTraceWithSpecifiedDepth() {
        test(-1);
    }

    private void test(Integer depth) {
        SimpleDateFormat sdf;
        String os = System.getProperty(OS_NAME_KEY).toLowerCase();
        // window system don't support ":" in file name
        if (os.contains(OS_WIN_PREFIX)) {
            sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss .SSS");
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss .SSS");
        }
        String dateStr = sdf.format(new Date());

        String property = System.getProperty(DUBBO_JSTACK_MAXLINE);
        System.setProperty(DUBBO_JSTACK_MAXLINE, depth.toString());
        try (FileOutputStream jStackStream =
                new FileOutputStream(new File("/tmp", "Dubbo_JStack.log" + "." + dateStr))) {
            jstack(jStackStream);
        } catch (Exception e) {
            logger.error(COMMON_UNEXPECTED_CREATE_DUMP, "", "", "dump jStack error", e);
        } finally {
            if (property == null) {
                System.clearProperty(DUBBO_JSTACK_MAXLINE);
            } else {
                System.setProperty(DUBBO_JSTACK_MAXLINE, property);
            }
        }
    }
}
