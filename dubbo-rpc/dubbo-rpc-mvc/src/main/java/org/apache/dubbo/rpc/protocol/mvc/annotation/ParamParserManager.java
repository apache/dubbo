package org.apache.dubbo.rpc.protocol.mvc.annotation;


import org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse.ProviderParamParser;

import java.util.Arrays;
import java.util.List;

public class ParamParserManager {


    // TODO ExtendClassLoader load
    List<ProviderParamParser> paramParsers = Arrays.asList();

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
            for (ProviderParamParser paramParser : paramParsers) {

                paramParser.parse(parseContext, parseContext.getArgInfoByIndex(i));
            }
        }
        return args.toArray(new Object[0]);
    }
}
