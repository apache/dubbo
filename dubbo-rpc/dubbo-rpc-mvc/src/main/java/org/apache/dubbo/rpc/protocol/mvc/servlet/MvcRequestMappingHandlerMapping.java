package org.apache.dubbo.rpc.protocol.mvc.servlet;

import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;

public class MvcRequestMappingHandlerMapping extends RequestMappingHandlerMapping {


    public MvcRequestMappingHandlerMapping() {
    }

    public void parseHandler(Class handler, Object handlerImpl) {


        Map<Method, RequestMappingInfo> methods = MethodIntrospector.selectMethods(handler,
            (MethodIntrospector.MetadataLookup) method -> {
                try {
                    return getMappingForMethod(method, handler);
                } catch (Throwable ex) {
                    throw new IllegalStateException("Invalid mapping on handler class [" +
                        handler.getName() + "]: " + method, ex);
                }
            });


        methods.forEach((method, mapping) -> {
            Method invocableMethod = MethodIntrospector.selectInvocableMethod(method, handler);
            registerHandlerMethod(handlerImpl, invocableMethod, mapping);
        });
    }

    public void parseHandler(Object handlerImpl) {
        parseHandler(handlerImpl.getClass(), handlerImpl);
    }


    public void unregister(Class handler) {

    }


}
