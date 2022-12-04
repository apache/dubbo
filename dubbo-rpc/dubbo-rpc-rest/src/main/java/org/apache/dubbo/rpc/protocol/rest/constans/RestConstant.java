package org.apache.dubbo.rpc.protocol.rest.constans;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.Constants;

public interface RestConstant {
    String INTERFACE = CommonConstants.INTERFACE_KEY;
    String METHOD = CommonConstants.METHOD_KEY;
    String PARAMETER_TYPES_DESC = CommonConstants.GENERIC_PARAMETER_DESC;
    String VERSION = CommonConstants.VERSION_KEY;
    String GROUP = CommonConstants.GROUP_KEY;
    String PATH = CommonConstants.PATH_KEY;
    String HOST = CommonConstants.HOST_KEY;
    String LOCAL_ADDR = "LOCAL_ADDR";
    String REMOTE_ADDR = "REMOTE_ADDR";
    String LOCAL_PORT = "LOCAL_PORT";
    String REMOTE_PORT = "REMOTE_PORT";
    String SERIALIZATION_KEY = Constants.SERIALIZATION_KEY;
    String PROVIDER_BODY_PARSE = "body";
    String PROVIDER_PARAM_PARSE = "param";
    String PROVIDER_HEADER_PARSE = "header";
    String PROVIDER_PATH_PARSE = "path";
    String PROVIDER_REQUEST_PARSE = "reuqest";
    String DUBBO_ATTACHMENT_HEADER = "Dubbo-Attachments";
    int MAX_HEADER_SIZE = 8 * 1024;

    String ADD_MUST_ATTTACHMENT = "must-intercept";
    String RPCCONTEXT_INTERCEPT = "rpc-context";
    String SERIALIZE_INTERCEPT = "serialize";
}
