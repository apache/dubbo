package org.apache.dubbo.remoting.transport.netty4.stub;

import java.util.List;

import io.netty.channel.EventLoopGroup;
import org.reactivestreams.Subscriber;

public interface ServerMethodModel extends MethodModel {

    boolean isUnary();

    Subscriber<Object> streamInvoke(Subscriber<Object> respObserver);

    List<String> getPaths();

    EventLoopGroup eventloopGroup();
}
