package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;


import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParser;

@SPI()
public interface BaseProviderParamParser extends ParamParser<ProviderParseContext> {

}
