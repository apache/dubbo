package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.deploy.ApplicationDeployer;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

// Regression test for apache/dubbo#16397: when the path can't be resolved
// because the app is shutting down,it should return 503 (-> gRPC UNAVAILABLE,
// retriable) instead of 404 (-> gRPC UNIMPLEMENTED, non-retriable).
class GrpcRequestHandlerMappingTest {

    @Test
    void returnsServiceUnavailableWhenStoppingAndInvokerNotFound() {
        FrameworkModel frameworkModel = new FrameworkModel();
        GrpcRequestHandlerMapping mapping = new GrpcRequestHandlerMapping(frameworkModel);

        ApplicationDeployer deployer = Mockito.mock(ApplicationDeployer.class);
        Mockito.when(deployer.isStopping()).thenReturn(true);
        ApplicationModel applicationModel = Mockito.mock(ApplicationModel.class);
        Mockito.when(applicationModel.getDeployer()).thenReturn(deployer);

        URL url = Mockito.mock(URL.class);
        Mockito.when(url.getOrDefaultApplicationModel()).thenReturn(applicationModel);

        HttpRequest request = Mockito.mock(HttpRequest.class);
        Mockito.when(request.contentType()).thenReturn("application/grpc");
        Mockito.when(request.uri()).thenReturn("/DemoService/sayHello");

        HttpStatusException ex = Assertions.assertThrows(
                HttpStatusException.class,
                () -> mapping.getRequestHandler(url, request, Mockito.mock(HttpResponse.class)));

        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE.getCode(), ex.getStatusCode());
    }
}
