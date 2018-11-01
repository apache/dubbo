package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

/**
 * @author cvictory ON 2018/10/25
 */
public class MetadataIdentifier {
    public static final String SEPARATOR = ":";
    final static String DEFAULT_PATH_TAG = "metadata";

    private String serviceInterface;
    private String version;
    private String group;
    private String side;

    public MetadataIdentifier() {
    }

    public MetadataIdentifier(String serviceInterface, String version, String group, String side) {
        this.serviceInterface = serviceInterface;
        this.version = version;
        this.group = group;
        this.side = side;
    }

    public String getIdentifierKey() {
        return serviceInterface + SEPARATOR + version + SEPARATOR + group + SEPARATOR + side;
    }

    public String getFilePathKey() {
        return getFilePathKey(DEFAULT_PATH_TAG);
    }

    public String getFilePathKey(String pathTag) {
        return toServicePath() + Constants.PATH_SEPARATOR + pathTag + Constants.PATH_SEPARATOR + (version == null ? "" : (version + Constants.PATH_SEPARATOR))
                + side + getPathSegment();
    }

    private String toServicePath() {
        if (Constants.ANY_VALUE.equals(serviceInterface)) {
            return "";
        }
        return URL.encode(serviceInterface);
    }

    protected String getPathSegment() {
        return "";
    }


    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }
}
