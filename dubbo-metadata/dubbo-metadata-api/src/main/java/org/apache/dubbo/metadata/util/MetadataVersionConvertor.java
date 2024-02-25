package org.apache.dubbo.metadata.util;

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataInfo.ServiceInfo;
import org.apache.dubbo.metadata.MetadataInfoV2;
import org.apache.dubbo.metadata.ServiceInfoV2;

import java.util.HashMap;
import java.util.Map;

public class MetadataVersionConvertor {

    public static MetadataInfoV2 toV2(MetadataInfo metadataInfo){
        if(metadataInfo == null){
            return MetadataInfoV2.newBuilder().build();
        }
        Map<String, ServiceInfoV2> servicesV2 = new HashMap<>();

        metadataInfo.getServices().forEach((name,serviceInfo) -> servicesV2.put(name,toV2(serviceInfo)));
        return MetadataInfoV2.newBuilder()
                .setVersion(metadataInfo.getRevision())
                .setApp(metadataInfo.getApp())
                .putAllServices(servicesV2).build();
    }

    public static ServiceInfoV2 toV2(ServiceInfo serviceInfo){
        if(serviceInfo == null){
            return ServiceInfoV2.newBuilder().build();
        }
        return ServiceInfoV2.newBuilder()
                .setVersion(serviceInfo.getVersion())
                .setGroup(serviceInfo.getGroup())
                .setName(serviceInfo.getName())
                .setPort(serviceInfo.getPort())
                .setPath(serviceInfo.getPath())
                .setProtocol(serviceInfo.getProtocol())
                .putAllParams(serviceInfo.getAllParams())
                .build();
    }

}
