package org.apache.dubbo.rpc.protocol.rsocket;

/**
 * @author sixie.xyn on 2019/1/3.
 */
public class RSocketConstants {

    public static final String SERVICE_NAME_KEY = "_service_name";
    public static final String SERVICE_VERSION_KEY = "_service_version";
    public static final String METHOD_NAME_KEY = "_method_name";
    public static final String PARAM_TYPE_KEY = "_param_type";
    public static final String SERIALIZE_TYPE_KEY = "_serialize_type";
    public static final String TIMEOUT_KEY = "_timeout";


    public static final int FLAG_ERROR = 0x01;
    public static final int FLAG_NULL_VALUE = 0x02;
    public static final int FLAG_HAS_ATTACHMENT = 0x04;
}
