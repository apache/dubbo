package org.apache.dubbo.common.serialize;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

@SPI(scope = ExtensionScope.FRAMEWORK)
public interface SerializationFilter {

    void filterRequest(URL url, String methodName, Class<?>[] requestTypes);

    void filterResponse(URL url, String methodName, Class<?> responseType);

}
