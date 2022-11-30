package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

/**
 *  Http Parameter param parse
 */
@Activate(value = RestConstant.PROVIDER_PARAM_PARSE)
public class ParamProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        //TODO MAP<String,String> convert
        ServletRequestFacade request = parseContext.getRequestFacade();

        String param = request.getParameter(argInfo.getAnnoNameAttribute());

        Object paramValue = paramTypeConvert(argInfo.getParamType(), param);
        parseContext.setValueByIndex(argInfo.getIndex(), paramValue);
    }

    @Override
    protected ParamType getParamType() {
        return ParamType.PARAM;
    }
}
