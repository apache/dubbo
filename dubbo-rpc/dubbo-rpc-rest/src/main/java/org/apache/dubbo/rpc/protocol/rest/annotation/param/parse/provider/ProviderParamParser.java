package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;


import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;

public abstract class ProviderParamParser implements BaseProviderParamParser {

    public void parse(ProviderParseContext parseContext, ArgInfo argInfo) {

        if (!matchParseType(argInfo.getParamAnnotationType())) {
            return;
        }

        doParse(parseContext, argInfo);
    }

    protected abstract void doParse(ProviderParseContext parseContext, ArgInfo argInfo);

    public boolean matchParseType(Class paramAnno) {

        ParamType paramAnnotType = getParamType();
        return paramAnnotType.supportAnno(paramAnno);
    }

    protected abstract ParamType getParamType();

    protected <T> T castReqOrRes(Class<T> reqOrResClass, Object reqOrRes) {
        return reqOrResClass.cast(reqOrRes);
    }

    protected Object paramTypeConvert(Class targetType, String value) {


        return DataParseUtils.StringTypeConvert(targetType, value);

    }


}
