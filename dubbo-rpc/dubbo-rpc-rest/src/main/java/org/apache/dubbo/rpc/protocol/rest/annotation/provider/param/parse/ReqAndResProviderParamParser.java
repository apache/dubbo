package org.apache.dubbo.rpc.protocol.rest.annotation.provider.param.parse;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;

/**
 * request or response param parse
 */
@Activate(value = RestConstant.PROVIDER_REQUEST_PARSE)
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
