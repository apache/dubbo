package org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ArgInfo;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParamType;
import org.apache.dubbo.rpc.protocol.mvc.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.mvc.request.ServletRequestFacade;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Http Parameter param parse
 */
@Activate(value = RestConstant.PROVIDER_PARAM_PARSE)
public class ParamProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        //TODO MAP<String,String> convert
        ServletRequestFacade request = parseContext.getRequestFacade();

        if (Map.class.isAssignableFrom(argInfo.getParamType())) {

            Map<String, String> paramMap = new LinkedHashMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                paramMap.put(name, request.getParameter(name));
            }
            parseContext.setValueByIndex(argInfo.getIndex(), paramMap);
            return;

        }

        String param = request.getParameter(argInfo.getAnnoNameAttribute());

        Object paramValue = paramTypeConvert(argInfo.getParamType(), param);
        parseContext.setValueByIndex(argInfo.getIndex(), paramValue);
    }

    @Override
    protected ParamType getParamAnnotationType() {
        return ParamType.PARAM;
    }
}
