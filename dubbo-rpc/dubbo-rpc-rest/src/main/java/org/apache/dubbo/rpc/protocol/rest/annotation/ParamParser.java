package org.apache.dubbo.rpc.protocol.rest.annotation;

import org.apache.dubbo.metadata.rest.ArgInfo;


public interface ParamParser<T> {
    void parse(T parseContext, ArgInfo argInfo);
}
