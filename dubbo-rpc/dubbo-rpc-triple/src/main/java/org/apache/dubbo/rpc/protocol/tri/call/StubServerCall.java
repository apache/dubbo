package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStreamListener;

import com.google.protobuf.Message;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

public class StubServerCall extends ServerCall {
    private final StubMethodDescriptor methodDescriptor;

    public StubServerCall(Invoker<?> invoker,
        ServerStream serverStream,
        FrameworkModel frameworkModel,
        String serviceName,
        String methodName,
        Executor executor) {
        super(invoker, serverStream, frameworkModel, serviceName, methodName, executor);
        this.methodDescriptor = (StubMethodDescriptor) invoker.getUrl()
            .getServiceModel()
            .getServiceModel()
            .getMethods(methodName)
            .get(0);
    }

    @Override
    public ServerStreamListener doStartCall(Map<String, Object> metadata) {
        RpcInvocation invocation = buildInvocation(metadata, methodDescriptor);
        listener = ServerCallUtil.startCall(this, invocation, methodDescriptor, invoker);
        return new ServerStreamListenerImpl();
    }


    @Override
    protected byte[] packResponse(Object message) {
        return ((Message) message).toByteArray();
    }

    class ServerStreamListenerImpl extends ServerCall.ServerStreamListenerBase {

        @Override
        public void complete() {
            listener.onComplete();
        }

        @Override
        public void cancel(TriRpcStatus status) {
            listener.onCancel(status.description);
        }

        @Override
        protected void doOnMessage(byte[] message) throws IOException, ClassNotFoundException {
            Object request = methodDescriptor.parseRequest(message);
            listener.onMessage(request);
        }
    }
}
