package org.apache.dubbo.config.spring.extension;

import org.apache.dubbo.config.DubboShutdownHook;
import org.junit.jupiter.api.Test;

public class GraceFulShutDownHookTest {

    @Test
    public void testGraceFulShutDownHook(){
        DubboShutdownHook hook = DubboShutdownHook.getDubboShutdownHook();
        hook.doDestroy();
    }
}
