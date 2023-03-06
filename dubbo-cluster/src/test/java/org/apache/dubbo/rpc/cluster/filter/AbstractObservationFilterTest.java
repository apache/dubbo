package org.apache.dubbo.rpc.cluster.filter;

import io.micrometer.tracing.test.SampleTestRunner;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

abstract class AbstractObservationFilterTest extends SampleTestRunner {

    ApplicationModel applicationModel;
    RpcInvocation    invocation;

    BaseFilter filter;

    Invoker<?> invoker = mock(Invoker.class);

    static final String INTERFACE_NAME = "org.apache.dubbo.MockInterface";
    static final String METHOD_NAME = "mockMethod";
    static final String GROUP = "mockGroup";
    static final String VERSION = "1.0.0";

    @AfterEach
    public void teardown() {
        if (applicationModel != null) {
            applicationModel.destroy();
        }
    }

    abstract BaseFilter createFilter(ApplicationModel applicationModel);

    void setupConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockObservations");

        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

        invocation = new RpcInvocation(new MockInvocation());
        invocation.addInvokedInvoker(invoker);

        applicationModel.getBeanFactory().registerBean(getObservationRegistry());

        filter = createFilter(applicationModel);

        given(invoker.invoke(invocation)).willReturn(new AppResponse("success"));

        initParam();
    }

    private void initParam() {
        invocation.setTargetServiceUniqueName(GROUP + "/" + INTERFACE_NAME + ":" + VERSION);
        invocation.setMethodName(METHOD_NAME);
        invocation.setParameterTypes(new Class[] {String.class});
    }

}
