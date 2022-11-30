package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

import java.util.Map;

/**
 * header param parse
 */
@Activate(value = RestConstant.PROVIDER_HEADER_PARSE)
public class HeaderProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        //TODO MAP<String,String> convert
        ServletRequestFacade request = parseContext.getRequestFacade();


        String header = request.getHeader(argInfo.getAnnoNameAttribute());
        Object headerValue = paramTypeConvert(argInfo.getParamType(), header);


        parseContext.setValueByIndex(argInfo.getIndex(), headerValue);

    }

    @Override
    protected ParamType getParamType() {
        return ParamType.HEADER;
    }
}
