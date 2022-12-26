package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * header param parse
 */
@Activate(value = RestConstant.PROVIDER_HEADER_PARSE)
public class HeaderProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ProviderParseContext parseContext, ArgInfo argInfo) {

        //TODO MAP<String,String> convert
        RequestFacade request = parseContext.getRequestFacade();
        if (Map.class.isAssignableFrom(argInfo.getParamType())) {

            Map<String, String> headerMap = new LinkedHashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headerMap.put(name, request.getHeader(name));
            }
            parseContext.setValueByIndex(argInfo.getIndex(), headerMap);
            return;

        }


        String header = request.getHeader(argInfo.getAnnotationNameAttribute());
        Object headerValue = paramTypeConvert(argInfo.getParamType(), header);


        parseContext.setValueByIndex(argInfo.getIndex(), headerValue);

    }

    @Override
    protected ParamType getParamType() {
        return ParamType.HEADER;
    }
}
