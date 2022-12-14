package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParseContext;

@SPI()
public interface ParamParser {
    void parse(ParseContext parseContext, ArgInfo argInfo);
}
