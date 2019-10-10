package org.apache.dubbo.metadata.report.identifier;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_PATH_TAG;
import static org.apache.dubbo.metadata.MetadataConstants.KEY_SEPARATOR;

/**
 * The Base class of MetadataIdentifier for service scope
 * <p>
 * 2019-08-09
 */
public class BaseApplicationMetadataIdentifier {
    String application;

    String getUniqueKey(KeyTypeEnum keyType, String... params) {
        if (keyType == KeyTypeEnum.PATH) {
            return getFilePathKey(params);
        }
        return getIdentifierKey(params);
    }

    String getIdentifierKey(String... params) {

        return application
                + joinParams(KEY_SEPARATOR, params);
    }

    private String joinParams(String joinChar, String... params) {
        if (params == null || params.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String param : params) {
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
                + application
                + joinParams(PATH_SEPARATOR, params);
    }

}
