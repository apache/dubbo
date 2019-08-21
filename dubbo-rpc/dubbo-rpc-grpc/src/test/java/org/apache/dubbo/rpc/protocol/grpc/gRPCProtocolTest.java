package org.apache.dubbo.rpc.protocol.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class gRPCProtocolTest {
    private static final Logger logger = LoggerFactory.getLogger(gRPCProtocolTest.class);

    @Test
    public void testUnaryRPC() throws IOException {
        HelloServiceImpl service = new HelloServiceImpl();
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        URL url = URL.valueOf(gRPCProtocol.NAME + "://127.0.0.1:50051/" + service.toString());
        Exporter<GreeterGrpc.IGreeter> exporter = protocol
                .export(proxyFactory.getInvoker(service, GreeterGrpc.IGreeter.class, url));

        Invoker<GreeterGrpc.IGreeter> invoker = protocol.refer(GreeterGrpc.IGreeter.class, url);
        GreeterGrpc.IGreeter client = proxyFactory.getProxy(invoker);
        HelloService.HelloRequest request = HelloService.HelloRequest.newBuilder().setRequestData("Zhou Yang").build();
        StreamObserver<HelloService.HelloResponse> streamObserver = new StreamObserver<HelloService.HelloResponse>() {
            @Override
            public void onNext(HelloService.HelloResponse helloResponse) {
                logger.info(helloResponse.getResponseData());
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        };
        new Thread(() -> client.helloWorld(request, streamObserver)).start();
        System.in.read();
        invoker.destroy();
        exporter.unexport();
    }

    @Test
    public void testgRPC() {
        HelloService.HelloRequest request = HelloService.HelloRequest.newBuilder().setRequestData("hhh").build();
        ManagedChannel channel = ManagedChannelBuilder.forTarget("dns:///localhost:8848").usePlaintext().build();
//        ManagedChannel channel = ManagedChannelBuilder.forAddress("127.0.0.1", 50051).usePlaintext().build();
        GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);
        HelloService.HelloResponse response = blockingStub.helloWorld(request);
        System.out.println(response.getResponseData());
    }
}
