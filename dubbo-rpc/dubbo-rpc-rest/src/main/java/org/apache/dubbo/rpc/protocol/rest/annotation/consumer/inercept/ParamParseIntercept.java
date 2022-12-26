package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer.ConsumerParseContext;
@Activate("paramparse")
public class ParamParseIntercept implements HttpConnectionPreBuildIntercept {
    private static final ParamParserManager paramParser = new ParamParserManager();

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        ConsumerParseContext consumerParseContext = new ConsumerParseContext();
        consumerParseContext.setArgInfos(consumerParseContext.getArgInfos());
        consumerParseContext.setArgs(consumerParseContext.getArgs());
        paramParser.consumerParamParse(consumerParseContext);
    }
}
