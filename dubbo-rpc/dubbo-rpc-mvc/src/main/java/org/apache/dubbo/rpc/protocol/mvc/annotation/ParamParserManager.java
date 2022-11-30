package org.apache.dubbo.rpc.protocol.mvc.annotation;


import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse.ParamParser;
import org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse.ProviderParamParser;

import java.util.List;
import java.util.Set;

public class ParamParserManager {



    Set<ParamParser> paramParsers = ApplicationModel.defaultModel().getExtensionLoader(ParamParser.class).getSupportedExtensionInstances();

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
    public Object[] providerParamParse(ParseContext parseContext) {

        List<Object> args = parseContext.getArgs();

        for (int i = 0; i < args.size(); i++) {
            for (ParamParser paramParser : paramParsers) {

                paramParser.parse(parseContext, parseContext.getArgInfoByIndex(i));
            }
        }
        return args.toArray(new Object[0]);
    }
}
