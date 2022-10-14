package org.apache.dubbo.config;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class DubboShutdownHookTest {
    private DubboShutdownHook dubboShutdownHook;
    private ApplicationModel applicationModel;

    @BeforeEach
    public void init() {
        SysProps.setProperty(CommonConstants.IGNORE_LISTEN_SHUTDOWN_HOOK, "false");
        FrameworkModel frameworkModel = new FrameworkModel();
        applicationModel = new ApplicationModel(frameworkModel);
        ModuleModel moduleModel = applicationModel.newModule();
        dubboShutdownHook = new DubboShutdownHook(applicationModel);
    }

    @AfterEach
    public void clear() {
        SysProps.clear();
    }

    @Test
    public void testDubboShutdownHook() {
        Assertions.assertNotNull(dubboShutdownHook);
        Assertions.assertLinesMatch(asList("DubboShutdownHook"), asList(dubboShutdownHook.getName()));
        Assertions.assertFalse(dubboShutdownHook.getRegistered());
    }

    @Test
    public void testDestoryNoModuleManagedExternally() {
        dubboShutdownHook.run();
        boolean hasModuleManagedExternally = false;
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            if (moduleModel.isLifeCycleManagedExternally()) {
                hasModuleManagedExternally = true;
                break;
            }
        }
        Assertions.assertFalse(hasModuleManagedExternally);
        Assertions.assertTrue(applicationModel.isDestroyed());
    }

    @Test
    public void testDestoryWithModuleManagedExternally() throws InterruptedException {
        applicationModel.getModuleModels().get(0).setLifeCycleManagedExternally(true);
        new Thread(() -> {
            applicationModel.getModuleModels().get(0).destroy();
        }).start();
        TimeUnit.MILLISECONDS.sleep(10);
        dubboShutdownHook.run();
        Assertions.assertTrue(applicationModel.isDestroyed());
    }
}
