package org.apache.dubbo.xds.security.authz.rule;

public class AuthorizationPolicyPathConvertor {

    public static RequestAuthProperty convert(String path){

        switch (path){
            case "rules.to.operation.paths":
                return RequestAuthProperty.URL_PATH;
            case "rules.to.operation.methods":
                return RequestAuthProperty.METHODS;
            case "rules.from.source.namespaces":
                return RequestAuthProperty.SOURCE_NAMESPACE;
            case "rules.source.service.name":
                return RequestAuthProperty.SOURCE_SERVICE_NAME;
            case "rules.source.service.uid":
                return RequestAuthProperty.SOURCE_SERVICE_UID;
            case "rules.source.pod.name":
                return RequestAuthProperty.SOURCE_POD_NAME;
            case "rules.source.pod.id":
                return RequestAuthProperty.SOURCE_POD_ID;
            case "rules.from.source.principals":
                return RequestAuthProperty.SERVICE_PRINCIPAL;
            case "rules.to.operation.version":
                return RequestAuthProperty.TARGET_VERSION;
                default:
                    throw new RuntimeException("not supported path:"+path);
        }
    }
}
