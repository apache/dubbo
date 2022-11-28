package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;

import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

public class HeaderProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        ServletRequestFacade request = parseContext.getRequest();

        String header = request.getHeader(argInfo.getAnnoNameAttribute());

        parseContext.setValueByIndex(argInfo.getIndex(), header);

    }

    @Override
    protected ParamType getParamType() {
        return ParamType.HEADER;
    }
}
