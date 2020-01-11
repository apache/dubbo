package org.apache.dubbo.remoting.pilot.option;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import io.envoyproxy.envoy.api.v2.ClusterLoadAssignment;
import io.envoyproxy.envoy.api.v2.DiscoveryRequest;
import io.envoyproxy.envoy.api.v2.DiscoveryResponse;
import io.envoyproxy.envoy.api.v2.core.Metadata;
import io.envoyproxy.envoy.api.v2.core.Node;
import io.envoyproxy.envoy.api.v2.core.SocketAddress;
import io.envoyproxy.envoy.api.v2.endpoint.Endpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LbEndpoint;
import io.envoyproxy.envoy.api.v2.endpoint.LocalityLbEndpoints;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovery model transfer util
 * @author hzj
 * @date 2019/03/20
 */
public class DiscoveryUtil {

    private final static String NODE_ID = "sidecar~127.0.0.1~dubbo.default~default.svc.cluster.local";
    private final static String RESOURCE_NAME_PREFIX = "outbound|0||";
    private final static String RESOURCE_NAME_SUFFIX = ".apache.dubbo.service.svc.cluster.local";
    private final static String TYPE_URL = "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment";
    private final static String ISTIO_KEY = "istio";
    private final static String SERVICE_NAME = "service_name";
    private final static String PROTOCOL_KEY = "protocol";
    private final static String GROUP_KEY = "service_group";
    private final static String VERSION_KEY = "service_version";

    /**
     * warm up ClusterLoadAssignment.class
     */
    public static void init() {
        ClusterLoadAssignment.getDefaultInstance().getDescriptorForType();
    }

    /**
     * Example Struct of DiscoveryRequest
     * {
     *     "versionInfo":"2019-03-06T15:44:58+08:00/1455",
     *     "resources":[
     *         {
     *             "@type":"type.googleapis.com/envoy.api.v2.ClusterLoadAssignment",
     *             "clusterName":"outbound|0||apache-dubbo-ChargeService.apache.dubbo.service.svc.cluster.local",
     *             "endpoints":[
     *                 {
     *                     "locality":{
     *                         "zone":"prod"
     *                     },
     *                     "lbEndpoints":[
     *                         {
     *                             "endpoint":{
     *                                 "address":{
     *                                     "socketAddress":{
     *                                         "address":"11.22.33.44",
     *                                         "portValue":7100
     *                                     }
     *                                 }
     *                             },
     *                             "metadata":{
     *                                 "filterMetadata":{
     *                                     "istio":{
     *                                         "service_name":"apache.dubbo.demo.Test1",
     *                                         "app_name":"test",
     *                                         "namespace":"etcd3",
     *                                         "protocol":"dubbo",
     *                                         "weight":100,
     *                                         "health_status":"UP",
     *                                         "service_version":"v1.0.0",
     *                                         "service_group":"default"
     *                                         .......
     *                                     }
     *                                 }
     *                             }
     *                         }
     *                     ]
     *                 }
     *             ]
     *         }
     *     ],
     *     "typeUrl":"type.googleapis.com/envoy.api.v2.ClusterLoadAssignment",
     *     "nonce":"fbf48761-bfd4-4041-b492-2ff0e1e3fabc"
     * }
     * @param url
     * @return
     */
    public static DiscoveryRequest urlToDiscoveryRequest(URL url) {
        DiscoveryRequest.Builder builder = DiscoveryRequest.newBuilder();
        Node node = urlToDiscoveryRequestNode(url);
        String resourceName = urlToDiscoveryRequestResourceName(url);
        String typeUrl = urlToDiscoveryRequestTypeUrl(url);
        builder.setNode(node);
        builder.addResourceNamesBytes(ByteString.copyFromUtf8(resourceName));
        builder.setTypeUrl(typeUrl);
        return builder.build();
    }

    /**
     * transfer url to Node
     * @param url
     * @return
     */
    public static Node urlToDiscoveryRequestNode(URL url) {
        Node.Builder builder = Node.newBuilder();
        String realNodeId = NODE_ID.replace("127.0.0.1", url.getHost());
        builder.setId(realNodeId);
        return builder.build();
    }

    /**
     * transfer url to ResourceName
     * @param url
     * @return
     */
    public static String urlToDiscoveryRequestResourceName(URL url) {
        return RESOURCE_NAME_PREFIX + url.getServiceInterface().replace('.', '-') + RESOURCE_NAME_SUFFIX;
    }


    /**
     * transfer url to TypeURL
     * @param url
     * @return
     */
    public static String urlToDiscoveryRequestTypeUrl(URL url) {
        return TYPE_URL;
    }

    /**
     * transfer DiscoveryResponse to urls
     * @param response
     * @return
     * @throws InvalidProtocolBufferException
     */
    public static Map<String, List<URL>> discoveryResponseToUrls(DiscoveryResponse response) throws InvalidProtocolBufferException {
        Map<String, List<URL>> responses = new HashMap<>();
        List<com.google.protobuf.Any> resources = response.getResourcesList();
        for (Any any : resources) {
            ClusterLoadAssignment clusterLoadAssignment = any.unpack(ClusterLoadAssignment.class);
            String service = parseServiceFromClusterName(clusterLoadAssignment.getClusterName());
            List<URL> urls = discoveryClusterLoadAssignmentToUrl(clusterLoadAssignment);
            responses.put(service, urls);
        }
        return responses;
    }

    private static String parseServiceFromClusterName(String clusterName){
        return clusterName
                .replace(RESOURCE_NAME_PREFIX, "")
                .replace(RESOURCE_NAME_SUFFIX, "")
                .replace('-', '.')
                .trim();
    }

    /**
     * transfer clusterLoadAssignment to urls
     * @param clusterLoadAssignment
     * @return
     */
    private static List<URL> discoveryClusterLoadAssignmentToUrl(ClusterLoadAssignment clusterLoadAssignment) {
        List<URL> urls = new ArrayList<>();
        List<LocalityLbEndpoints> localityLbEndpoints = clusterLoadAssignment.getEndpointsList();
        localityLbEndpoints.forEach(localityLbEndpoint -> {
            String dc = localityLbEndpoint.getLocality().getZone();
            List<LbEndpoint> lbEndpoints = localityLbEndpoint.getLbEndpointsList();
            lbEndpoints.forEach(lbEndpoint -> {
                URL url = discoveryLbEndPointToUrl(lbEndpoint);
                url.addParameter("dc", dc);
                urls.add(url);
            });
        } );
        return urls;
    }

    private static URL discoveryLbEndPointToUrl(LbEndpoint lbEndpoint) {
        Map<String, String> parameters = new HashMap<>();
        /**
         * parse endpoint
         */
        Endpoint endpoint = lbEndpoint.getEndpoint();
        SocketAddress socketAddress = endpoint.getAddress().getSocketAddress();
        String host = socketAddress.getAddress();
        Integer port = socketAddress.getPortValue();
        if (host == null || host.isEmpty() || port == 0) {
            throw new RuntimeException("Invalid Socket address from pilot, host or port can not be empty");
        }

        /**
         * parse metadata
         */
        Metadata metadata = lbEndpoint.getMetadata();
        Map<String, Value> ext = metadata.getFilterMetadataMap().get(ISTIO_KEY).getFieldsMap();

        /**
         * ignore some fields
         */
        String protocol = ext.get(PROTOCOL_KEY).getStringValue();
        String service = ext.get(SERVICE_NAME).getStringValue();
        parameters.put(CommonConstants.PROTOCOL_KEY, protocol);
        parameters.put(CommonConstants.INTERFACE_KEY, service);
        if (StringUtils.isNotEmpty(ext.get(GROUP_KEY).getStringValue())) {
            parameters.put(CommonConstants.GROUP_KEY, ext.get(GROUP_KEY).getStringValue());
        }
        if (StringUtils.isNotEmpty(ext.get(VERSION_KEY).getStringValue())) {
            parameters.put(CommonConstants.VERSION_KEY, ext.get(VERSION_KEY).getStringValue());
        }
        return new URL(protocol, host, port, service, parameters);
    }
}
