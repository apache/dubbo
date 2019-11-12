package org.apache.dubbo.metadata.report.identifier;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_PATH_TAG;
import static org.apache.dubbo.metadata.MetadataConstants.KEY_SEPARATOR;

/**
 * The Base class of MetadataIdentifier for service scope
 * <p>
 * 2019-08-09
 */
public class BaseServiceMetadataIdentifier {
    String serviceInterface;
    String version;
    String group;
    String side;

    String getUniqueKey(KeyTypeEnum keyType, String... params) {
        if (keyType == KeyTypeEnum.PATH) {
            return getFilePathKey(params);
        }
        return getIdentifierKey(params);
    }

    String getIdentifierKey(String... params) {

        return serviceInterface
                + KEY_SEPARATOR + (version == null ? "" : version)
                + KEY_SEPARATOR + (group == null ? "" : group)
                + KEY_SEPARATOR + (side == null ? "" : side)
                + joinParams(KEY_SEPARATOR, params);
    }

    private String joinParams(String joinChar, String... params) {
        if (params == null || params.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            if (param == null) {
                continue;
            }
            sb.append(joinChar);
            sb.append(param);
        }
        return sb.toString();
    }

    private String getFilePathKey(String... params) {
        return getFilePathKey(DEFAULT_PATH_TAG, params);
    }

    private String getFilePathKey(String pathTag, String... params) {
        return pathTag
                + (StringUtils.isEmpty(toServicePath()) ? "" : (PATH_SEPARATOR + toServicePath()))
                + (version == null ? "" : (PATH_SEPARATOR + version))
                + (group == null ? "" : (PATH_SEPARATOR + group))
                + (side == null ? "" : (PATH_SEPARATOR + side))
                + joinParams(PATH_SEPARATOR, params);
    }

    private String toServicePath() {
        if (ANY_VALUE.equals(serviceInterface)) {
            return "";
        }
        return URL.encode(serviceInterface);
    }
}
