package org.apache.dubbo.demo.hello;

import com.google.protobuf.Message;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.model.StubServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.reactive.handler.ManyToManyMethodHandler;
import org.apache.dubbo.rpc.protocol.tri.reactive.handler.ManyToOneMethodHandler;
import org.apache.dubbo.rpc.protocol.tri.reactive.handler.OneToManyMethodHandler;
import org.apache.dubbo.rpc.protocol.tri.reactive.calls.ReactorClientCalls;
import org.apache.dubbo.rpc.protocol.tri.reactive.handler.OneToOneMethodHandler;
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

    private static final StubMethodDescriptor testOneToOneMethod = new StubMethodDescriptor("testOneToOne",
        org.apache.dubbo.demo.hello.HelloRequest.class, org.apache.dubbo.demo.hello.HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.UNARY,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), org.apache.dubbo.demo.hello.HelloRequest::parseFrom,
        org.apache.dubbo.demo.hello.HelloReply::parseFrom);

    private static final StubMethodDescriptor testOneToManyMethod = new StubMethodDescriptor("testOneToMany",
        org.apache.dubbo.demo.hello.HelloRequest.class, org.apache.dubbo.demo.hello.HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.SERVER_STREAM,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), org.apache.dubbo.demo.hello.HelloRequest::parseFrom,
        org.apache.dubbo.demo.hello.HelloReply::parseFrom);

    private static final StubMethodDescriptor testManyToOneMethod = new StubMethodDescriptor("testManyToOne",
        org.apache.dubbo.demo.hello.HelloRequest.class, org.apache.dubbo.demo.hello.HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.CLIENT_STREAM,
        obj -> ((Message) obj).toByteArray(), obj -> ((Message) obj).toByteArray(), org.apache.dubbo.demo.hello.HelloRequest::parseFrom,
        org.apache.dubbo.demo.hello.HelloReply::parseFrom);

    private static final StubMethodDescriptor testManyToManyMethod = new StubMethodDescriptor("testManyToMany",
        org.apache.dubbo.demo.hello.HelloRequest.class, org.apache.dubbo.demo.hello.HelloReply.class, serviceDescriptor, MethodDescriptor.RpcType.BI_STREAM,
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
        public Mono<HelloReply> testOneToOne(Mono<HelloRequest> request) {
            return ReactorClientCalls.oneToOne(invoker, request, testOneToOneMethod);
        }

        @Override
        public Flux<HelloReply> testOneToMany(Mono<HelloRequest> request) {
            return ReactorClientCalls.oneToMany(invoker, request, testOneToManyMethod);
        }

        @Override
        public Mono<HelloReply> testManyToOne(Flux<HelloRequest> request) {
            return ReactorClientCalls.manyToOne(invoker, request, testManyToOneMethod);
        }

        @Override
        public Flux<HelloReply> testManyToMany(Flux<HelloRequest> request) {
            return ReactorClientCalls.manyToMany(invoker, request, testManyToManyMethod);
        }
    }

    public static abstract class ReactorStreamTestImplBase implements ReactorStreamTest, ServerService<ReactorStreamTest> {

        @Override
        public final Invoker<ReactorStreamTest> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
                .getExtensionLoader(PathResolver.class)
                .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();

            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/testOneToOne");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/testOneToMany");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/testManyToOne");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/testManyToMany");

            handlers.put(testOneToOneMethod.getMethodName(), new OneToOneMethodHandler<>(this::testOneToOne));
            handlers.put(testOneToManyMethod.getMethodName(), new OneToManyMethodHandler<>(this::testOneToMany));
            handlers.put(testManyToOneMethod.getMethodName(), new ManyToOneMethodHandler<>(this::testManyToOne));
            handlers.put(testManyToManyMethod.getMethodName(), new ManyToManyMethodHandler<>(this::testManyToMany));

            return new StubInvoker<>(this, url, ReactorStreamTest.class, handlers);
        }

        @Override
        public Mono<HelloReply> testOneToOne(Mono<HelloRequest> requestMono) {
            throw unimplementedMethodException(testOneToOneMethod);
        }

        @Override
        public Flux<HelloReply> testOneToMany(Mono<HelloRequest> requestMono) {
            throw unimplementedMethodException(testOneToManyMethod);
        }

        @Override
        public Mono<HelloReply> testManyToOne(Flux<HelloRequest> requestFlux) {
            throw unimplementedMethodException(testManyToOneMethod);
        }

        @Override
        public Flux<HelloReply> testManyToMany(Flux<HelloRequest> requestFlux) {
            throw unimplementedMethodException(testManyToManyMethod);
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
