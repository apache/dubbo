package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;

/**
 * @author cvictory ON 2018/10/25
 */
public class ConsumerMetadataIdentifier extends MetadataIdentifier {

    public ConsumerMetadataIdentifier() {
    }

    public ConsumerMetadataIdentifier(String serviceInterface, String version, String group, String application) {
        super(serviceInterface, version, group, Constants.CONSUMER_SIDE);
        this.application = application;
    }

    private String application;

    protected String getPathSegment() {
        return Constants.PATH_SEPARATOR + application;
    }

    ;

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}
