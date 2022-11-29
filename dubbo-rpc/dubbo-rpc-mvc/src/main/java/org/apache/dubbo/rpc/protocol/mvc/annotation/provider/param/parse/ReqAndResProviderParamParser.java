package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;


import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;

/**
 *   request or response param parse
 */
public class ReqAndResProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {


        if (parseContext.isRequestArg(argInfo.getParamType())) {
            parseContext.setValueByIndex(argInfo.getIndex(), castReqOrRes(argInfo.getParamType(), parseContext.getRequest()));
        } else if (parseContext.isResponseArg(argInfo.getParamType())) {
            parseContext.setValueByIndex(argInfo.getIndex(), castReqOrRes(argInfo.getParamType(), parseContext.getResponse()));
        }
    }

    @Override
    protected ParamType getParamType() {
        return ParamType.REQ_OR_RES;
    }
}
