package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;


import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.util.NumberUtils;


public abstract class ProviderParamParser implements ParamParser {

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

    protected <T> T castReqOrRes(Class<T> reqOrResClass, Object reqOrRes) {
        return reqOrResClass.cast(reqOrRes);
    }

    protected Object paramTypeConvert(Class targetType, String value) {


        if (targetType == Boolean.class) {
            return Boolean.valueOf(value);
        }

        if (targetType == String.class) {
            return value;
        }

        if (Number.class.isAssignableFrom(targetType)) {
            return NumberUtils.parseNumber(value, targetType);
        }

        return value;

    }


}
