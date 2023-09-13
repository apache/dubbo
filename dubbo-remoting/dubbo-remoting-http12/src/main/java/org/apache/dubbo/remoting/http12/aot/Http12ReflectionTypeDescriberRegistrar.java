package org.apache.dubbo.remoting.http12.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;
import org.apache.dubbo.remoting.http12.netty4.HttpWriteQueueHandler;
import org.apache.dubbo.remoting.http12.netty4.h1.NettyHttp1Codec;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameCodec;
import org.apache.dubbo.remoting.http12.netty4.h2.NettyHttp2FrameHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Http12ReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        typeDescribers.add(buildTypeDescriberWithPublicMethod(HttpWriteQueueHandler.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyHttp1Codec.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyHttp2FrameCodec.class));
        typeDescribers.add(buildTypeDescriberWithPublicMethod(NettyHttp2FrameHandler.class));
        return typeDescribers;
    }

    private TypeDescriber buildTypeDescriberWithPublicMethod(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_PUBLIC_METHODS);
        return new TypeDescriber(cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
