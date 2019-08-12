package org.apache.dubbo.metadata.report.identifier;

import org.apache.dubbo.common.URL;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * The ServiceMetadataIdentifier is used to store the {@link org.apache.dubbo.common.URL}
 * that are from provider and consumer
 * <p>
 * 2019-08-09
 */
public class ServiceMetadataIdentifier extends BaseMetadataIdentifier {

    private String serviceInterface;
    private String version;
    private String group;
    private String side;
    private String revision;

    public ServiceMetadataIdentifier() {
    }

    public ServiceMetadataIdentifier(String serviceInterface, String version, String group, String side, String revision) {
        this.serviceInterface = serviceInterface;
        this.version = version;
        this.group = group;
        this.side = side;
        this.revision = revision;
    }


    public ServiceMetadataIdentifier(URL url) {
        this.serviceInterface = url.getServiceInterface();
        this.version = url.getParameter(VERSION_KEY);
        this.group = url.getParameter(GROUP_KEY);
        this.side = url.getParameter(SIDE_KEY);
        this.revision = (url.getParameter(REVISION_KEY));
    }

    public String getUniqueKey(MetadataIdentifier.KeyTypeEnum keyType) {
        return super.getUniqueKey(keyType, revision);
    }

    public String getIdentifierKey() {
        return super.getIdentifierKey(revision);
    }
}
