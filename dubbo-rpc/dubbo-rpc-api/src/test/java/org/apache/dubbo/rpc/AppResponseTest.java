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
package org.apache.dubbo.rpc;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AppResponseTest {
    @Test
    public void testAppResponseWithNormalException() {
        NullPointerException npe = new NullPointerException();
        AppResponse appResponse = new AppResponse(npe);

        StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 1);
    }

    /**
     * please run this test in Run mode
     */
    @Test
    public void testAppResponseWithEmptyStackTraceException() {
        Throwable throwable = buildEmptyStackTraceException();
        if (throwable == null) {
            return;
        }
        AppResponse appResponse = new AppResponse(throwable);

        StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertEquals(0,stackTrace.length);
    }

    @Test
    public void testSetExceptionWithNormalException() {
        NullPointerException npe = new NullPointerException();
        AppResponse appResponse = new AppResponse();
        appResponse.setException(npe);

        StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertTrue(stackTrace.length > 1);
    }

    /**
     * please run this test in Run mode
     */
    @Test
    public void testSetExceptionWithEmptyStackTraceException() {
        Throwable throwable = buildEmptyStackTraceException();
        if (throwable == null) {
            return;
        }
        AppResponse appResponse = new AppResponse();
        appResponse.setException(throwable);

        StackTraceElement[] stackTrace = appResponse.getException().getStackTrace();
        Assertions.assertNotNull(stackTrace);
        Assertions.assertEquals(0,stackTrace.length);
    }

    private Throwable buildEmptyStackTraceException() {
        // begin to construct a NullPointerException with empty stackTrace
        Throwable throwable = null;
        Long begin = System.currentTimeMillis();
        while (System.currentTimeMillis() - begin < 60000) {
            try {
                ((Object) null).getClass();
            } catch (Exception e) {
                if (e.getStackTrace().length == 0) {
                    throwable = e;
                    break;
                }
            }
        }
        /**
         * may be there is -XX:-OmitStackTraceInFastThrow or run in Debug mode
         */
        if (throwable == null) {
            System.out.println("###buildEmptyStackTraceException fail to construct NPE");
            return null;
        }
        // end construct a NullPointerException with empty stackTrace

        return throwable;
    }
}
