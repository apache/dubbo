package org.apache.dubbo.rpc.protocol.rest.annotation;


import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer.BaseConsumerParamParser;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer.ConsumerParseContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.BaseProviderParamParser;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.ProviderParseContext;

import java.util.List;
import java.util.Set;

public class ParamParserManager {


    private static final Set<BaseProviderParamParser> providerParamParsers =
        ApplicationModel.defaultModel().getExtensionLoader(BaseProviderParamParser.class).getSupportedExtensionInstances();


    private static final Set<BaseConsumerParamParser> consumerParamParsers =
        ApplicationModel.defaultModel().getExtensionLoader(BaseConsumerParamParser.class).getSupportedExtensionInstances();

    /**
     * provider  Design Description:
     * <p>
     * Object[] args=new Object[0];
     * List<Object> argsList=new ArrayList<>;</>
     * <p>
     * setValueByIndex(int index,Object value);
     * <p>
     * args=toArray(new Object[0]);
     */
    public Object[] providerParamParse(ProviderParseContext parseContext) {

        List<Object> args = parseContext.getArgs();

        for (int i = 0; i < args.size(); i++) {
            for (ParamParser paramParser : providerParamParsers) {

                paramParser.parse(parseContext, parseContext.getArgInfoByIndex(i));
            }
        }
        return args.toArray(new Object[0]);
    }

    public void consumerParamParse(ConsumerParseContext parseContext) {

        List<Object> args = parseContext.getArgs();

        for (int i = 0; i < args.size(); i++) {
            for (ParamParser paramParser : consumerParamParsers) {

                paramParser.parse(parseContext, parseContext.getArgInfoByIndex(i));
            }
        }

    }
}
