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
