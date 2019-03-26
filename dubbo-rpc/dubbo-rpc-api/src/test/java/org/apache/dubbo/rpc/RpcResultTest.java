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


import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

public class RpcResultTest {
    @Test
    public void testRecreateWithNormalException() {
        NullPointerException npe = new NullPointerException();
        RpcResult rpcResult = new RpcResult(npe);
        try {
            rpcResult.recreate();
            fail();
        } catch (Throwable throwable) {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            Assert.assertNotNull(stackTrace);
            Assert.assertTrue(stackTrace.length > 1);
        }
    }

    /**
     * please run this test in Run mode
     */
    @Test
    public void testRecreateWithEmptyStackTraceException() {
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
            System.out.println("###testRecreateWithEmptyStackTraceException fail to construct NPE");
            return;
        }
        // end construct a NullPointerException with empty stackTrace

        RpcResult rpcResult = new RpcResult(throwable);
        try {
            rpcResult.recreate();
            fail();
        } catch (Throwable t) {
            StackTraceElement[] stackTrace = t.getStackTrace();
            Assert.assertNotNull(stackTrace);
            Assert.assertTrue(stackTrace.length == 0);
        }
    }
}
