package org.apache.dubbo.parse;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.protocol.mvc.annotation.provider.param.parse.ParamParser;
import org.junit.jupiter.api.Test;

import java.util.Set;


public class ProviderParamParse {

    @Test
    public void testExtendClassLoader(){
        Set<ParamParser> supportedExtensionInstances = ExtensionLoader.getExtensionLoader(ParamParser.class).getSupportedExtensionInstances();

        System.out.println(supportedExtensionInstances);
    }
}
