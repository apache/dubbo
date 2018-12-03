package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

/**
 * 2018/10/25
 */
public class ConsumerMetadataIdentifier extends MetadataIdentifier {

    public ConsumerMetadataIdentifier() {
    }

    public ConsumerMetadataIdentifier(String serviceInterface, String version, String group, String application) {
        super(serviceInterface, version, group, Constants.CONSUMER_SIDE);
        this.application = application;
    }

    public ConsumerMetadataIdentifier(URL url) {
        super(url);
        setSide(Constants.CONSUMER_SIDE);
        setApplication(url.getParameter(Constants.APPLICATION_KEY));
    }

    private String application;

    protected String getPathSegment() {
        return Constants.PATH_SEPARATOR + application;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}
