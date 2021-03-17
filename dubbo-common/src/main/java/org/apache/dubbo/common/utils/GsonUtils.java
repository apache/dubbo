package org.apache.dubbo.common.utils;

import com.google.gson.Gson;

public final class GsonUtils {

    private final static Gson gson = new Gson();

    public static Gson getGson() {
        return gson;
    }

}
