package org.apache.dubbo.metadata.tri;

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.triple.metadata.AllMetaResponse;
import org.apache.dubbo.triple.metadata.MetaResponse;
import org.apache.dubbo.triple.metadata.ResponseStatus;
import org.apache.dubbo.triple.metadata.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataConverter {

    public static MetadataInfo triMetadataConvert(MetaResponse metaResponse) {
        if (!ResponseStatus.SUCCESS.equals(metaResponse.getStatus())) {
            throw new RpcException();
        }
        final String app = metaResponse.getApp();
        final String revision = metaResponse.getRevision();
        Map<String, MetadataInfo.ServiceInfo> serviceInfoMap = new ConcurrentHashMap<>();
        Map<String, Service> servicesMap = metaResponse.getServicesMap();
        servicesMap.forEach((serviceName, triService) -> {
            MetadataInfo.ServiceInfo serviceInfo = new MetadataInfo.ServiceInfo();
            serviceInfo.setGroup(triService.getGroup());
            serviceInfo.setName(triService.getName());
            serviceInfo.setPath(triService.getPath());
            serviceInfo.setProtocol(triService.getProtocol());
            serviceInfo.setVersion(triService.getVersion());
            serviceInfo.setPort(triService.getPort());
            serviceInfo.setParams(triService.getParamsMap());
            serviceInfoMap.put(serviceName, serviceInfo);
        });
        return new MetadataInfo(app, revision, serviceInfoMap);
    }

    public static List<MetadataInfo> triAllMetadataConvert(AllMetaResponse allMetaResponse) {
        List<MetadataInfo> metadataInfoList = new ArrayList<>();
        for (MetaResponse metaResponse : allMetaResponse.getAllMetadataList()) {
            metadataInfoList.add(triMetadataConvert(metaResponse));
        }
        return metadataInfoList;
    }
}
