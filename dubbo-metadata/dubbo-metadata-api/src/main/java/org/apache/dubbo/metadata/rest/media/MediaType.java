package org.apache.dubbo.metadata.rest.media;


public enum MediaType {
    ALL_VALUE("*/*"),
    APPLICATION_JSON_VALUE("application/json"),
    APPLICATION_FORM_URLENCODED_VALUE("application/x-www-form-urlencoded"),
    ;

    MediaType(String value) {
        this.value = value;
    }

    public String value;


}
