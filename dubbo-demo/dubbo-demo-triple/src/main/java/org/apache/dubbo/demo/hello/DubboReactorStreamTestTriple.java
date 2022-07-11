package org.apache.dubbo.demo.hello;

import com.google.protobuf.Message;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.model.StubServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.reactive.handler.OneToManyMethodHandler;
import org.apache.dubbo.rpc.protocol.tri.reactive.calls.ReactorClientCalls;
import org.apache.dubbo.rpc.stub.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

// auto-generated code. delete after writing mustache file
public final class DubboReactorStreamTestTriple {

    private DubboReactorStreamTestTriple() {}

    public static final String SERVICE_NAME = ReactorStreamTest.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME, ReactorStreamTest.class);

    static {
        StubSuppliers.addSupplier(SERVICE_NAME, DubboReactorStreamTestTriple::newStub);
        StubSuppliers.addSupplier(ReactorStreamTest.JAVA_SERVICE_NAME,  DubboReactorStreamTestTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(ReactorStreamTest.JAVA_SERVICE_NAME, serviceDescriptor);
    }


    private static final StubMethodDescriptor testMethod = new StubMethodDescriptor("test",
        HelloRequest.class, org.apache.dubbo.demo.hello.HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.SERVER_STREAM,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), org.apache.dubbo.demo.hello.HelloRequest::parseFrom,
        org.apache.dubbo.demo.hello.HelloReply::parseFrom);

    @SuppressWarnings("all")
    public static ReactorStreamTest newStub(Invoker<?> invoker) {
        return new ReactorDubboStreamTestStub((Invoker<ReactorStreamTest>) invoker);
    }

    public static final class ReactorDubboStreamTestStub implements ReactorStreamTest {

        private final Invoker<ReactorStreamTest> invoker;

        public ReactorDubboStreamTestStub(Invoker<ReactorStreamTest> invoker) {
            this.invoker = invoker;
        }

        @Override
        public Flux<HelloReply> test(Mono<HelloRequest> request) {
            return ReactorClientCalls.oneToMany(invoker, request, testMethod);
        }
    }

    public static abstract class ReactorStreamTestImplBase implements ReactorStreamTest, ServerService<ReactorStreamTest> {

        @Override
        public final Invoker<ReactorStreamTest> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(PathResolver.class)
                .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();

            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/test" );

            handlers.put(testMethod.getMethodName(), new OneToManyMethodHandler<>(this::test));

            return new StubInvoker<>(this, url, ReactorStreamTest.class, handlers);
        }

        public Flux<HelloReply> test(Mono<HelloRequest> requestMono) {
            throw unimplementedMethodException(testMethod);
        }

        @Override
        public final ServiceDescriptor getServiceDescriptor() {
            return serviceDescriptor;
        }
        private RpcException unimplementedMethodException(StubMethodDescriptor methodDescriptor) {
            return TriRpcStatus.UNIMPLEMENTED.withDescription(String.format("Method %s is unimplemented",
                "/" + serviceDescriptor.getInterfaceName() + "/" + methodDescriptor.getMethodName())).asException();
        }
    }
}
