package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer;


import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParser;

@SPI()
public interface BaseConsumerParamParser extends ParamParser<ConsumerParseContext> {

}
