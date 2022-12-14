package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;


/**
 * path param parse
 */
@Activate(value = RestConstant.PROVIDER_PATH_PARSE)
public class PathProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        String pathVariable = parseContext.getPathVariable(argInfo.getUrlSplitIndex());

        Object pathVariableValue = paramTypeConvert(argInfo.getParamType(), pathVariable);

        parseContext.setValueByIndex(argInfo.getIndex(), pathVariableValue);

    }

    @Override
    protected ParamType getParamType() {
        return ParamType.PATH;
    }
}
