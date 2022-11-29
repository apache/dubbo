package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;


import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

/**
 *  Http Parameter param parse
 */
public class ParamProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        //TODO primitive type convert  (number boolean param)
        ServletRequestFacade request = parseContext.getRequestFacade();

        String header = request.getParameter(argInfo.getAnnoNameAttribute());

        parseContext.setValueByIndex(argInfo.getIndex(), header);
    }

    @Override
    protected ParamType getParamType() {
        return ParamType.PARAM;
    }
}
