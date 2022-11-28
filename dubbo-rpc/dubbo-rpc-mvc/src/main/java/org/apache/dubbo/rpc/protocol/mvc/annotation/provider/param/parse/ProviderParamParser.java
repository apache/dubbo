package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;


import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;

public abstract class ProviderParamParser {

    public void parse(ParseContext parseContext, ArgInfo argInfo) {

        if (!matchParseType(argInfo.getParamAnno())) {
            return;
        }

        doParse(parseContext, argInfo);
    }

    protected abstract void doParse(ParseContext parseContext, ArgInfo argInfo);

    public boolean matchParseType(Class paramAnno) {

        ParamType paramAnnotType = getParamType();
        return paramAnnotType.supportAnno(paramAnno);
    }

    protected abstract ParamType getParamType();


}
