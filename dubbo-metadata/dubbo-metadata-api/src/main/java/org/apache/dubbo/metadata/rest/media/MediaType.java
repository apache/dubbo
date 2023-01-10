package org.apache.dubbo.metadata.rest.media;


public enum MediaType {
    ALL_VALUE("*/*"),
    APPLICATION_JSON_VALUE("application/json"),
    APPLICATION_FORM_URLENCODED_VALUE("application/x-www-form-urlencoded"),
    TEXT_PLAIN("text/plain"),
    ;

    MediaType(String value) {
        this.value = value;
    }

    public String value;

    public static String getAllContentType() {

        MediaType[] values = MediaType.values();

        StringBuilder stringBuilder = new StringBuilder();

        for (MediaType mediaType : values) {
            stringBuilder.append(mediaType.value + " ");
        }
        return stringBuilder.toString();
    }
}
