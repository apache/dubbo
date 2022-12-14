package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParseContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.io.InputStream;


/**
 * body param parse
 */
@Activate(value = RestConstant.PROVIDER_BODY_PARSE)
public class BodyProviderParamParser extends ProviderParamParser {
    private static final Logger logger = LoggerFactory.getLogger(BodyProviderParamParser.class);

    @Override
    protected void doParse(ParseContext parseContext, ArgInfo argInfo) {

        RequestFacade request = parseContext.getRequestFacade();

        try {
            String serialization = parseContext.getRequestFacade().getHeader(RestConstant.SERIALIZATION_KEY);
             //TODO MAP<String,String> convert
            // TODO  url builder
            URL url = null;
            // TODO json utils no stream convert
            // TODO add form parse
            InputStream inputStream = request.getInputStream();
            Serialization serializationById = CodecSupport.getSerializationById((Byte.parseByte(serialization)));
            ObjectInput deserialize = serializationById.deserialize(url, inputStream);
            Object o = deserialize.readObject(argInfo.getParamType());
            parseContext.setValueByIndex(argInfo.getIndex(), o);
        } catch (Exception e) {
            logger.error("BodyProviderParamParser parse error: {}", e);
        }
    }

    @Override
    protected ParamType getParamType() {
        return ParamType.BODY;
    }
}
