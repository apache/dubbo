package org.apache.dubbo.rpc.protocol.rest.annotation.metadata;

import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadataResolver;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.rest.exception.CodeStyleNotSupportException;

import java.util.Map;
import java.util.Set;

public class MetadataResolver {
    // TODO  choose resolver by url code Style ?
    private static Set<ServiceRestMetadataResolver> serviceRestMetadataResolvers =
        ApplicationModel.defaultModel().getExtensionLoader(ServiceRestMetadataResolver.class).getSupportedExtensionInstances();




    public static Map<PathMatcher, RestMethodMetadata> resolveProviderServiceMetadata(Class serviceImpl) {


        for (ServiceRestMetadataResolver serviceRestMetadataResolver : serviceRestMetadataResolvers) {
            boolean supports = serviceRestMetadataResolver.supports(serviceImpl);
            if (supports) {
                ServiceRestMetadata resolve = serviceRestMetadataResolver.resolve(serviceImpl);
                return resolve.getPathToServiceMap();
            }
        }
        throw new CodeStyleNotSupportException("service is:" + serviceImpl + ",just support rest or spring-web annotation");
    }
}
