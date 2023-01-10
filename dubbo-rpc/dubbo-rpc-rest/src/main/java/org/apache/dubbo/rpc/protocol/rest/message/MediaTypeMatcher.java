package org.apache.dubbo.rpc.protocol.rest.message;

import org.apache.dubbo.metadata.rest.media.MediaType;

import java.util.ArrayList;
import java.util.List;

public enum MediaTypeMatcher {


    MULTI_VALUE(createMediaList(MediaType.APPLICATION_FORM_URLENCODED_VALUE)),
    APPLICATION_JSON(createMediaList(MediaType.APPLICATION_JSON_VALUE)),
    TEXT_PLAIN(createMediaList(MediaType.TEXT_PLAIN)),

    ;

    private List<MediaType> mediaTypes;


    MediaTypeMatcher(List<MediaType> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }


    private static List<MediaType> createMediaList(MediaType... mediaTypes) {
        List<MediaType> mediaTypeList = getDefaultList();

        for (MediaType mediaType : mediaTypes) {

            mediaTypeList.add(mediaType);
        }
        return mediaTypeList;
    }

    private static List<MediaType> getDefaultList() {

        List<MediaType> defaultList = new ArrayList<>();
        defaultList.add(MediaType.ALL_VALUE);
        return defaultList;
    }

    public boolean mediaSupport(MediaType mediaType) {
        return mediaTypes.contains(mediaType);
    }


}
