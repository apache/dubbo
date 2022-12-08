package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;

@SPI()
public interface ParamParser {
    void parse(ParseContext parseContext, ArgInfo argInfo);
}
