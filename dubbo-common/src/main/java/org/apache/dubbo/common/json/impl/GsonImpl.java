//package org.apache.dubbo.common.json.impl;
//
//import org.apache.dubbo.common.json.JSON;
//
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//
//import java.lang.reflect.Type;
//import java.util.List;
//
//public class GsonImpl implements JSON {
//    private Gson gson = new Gson();
//
//    @Override
//    public <T> T toJavaObject(String json) {
//        return (T) gson.fromJson(json, Object.class);
//    }
//
//    @Override
//    public <T> T toJavaObject(String json, Type type) {
//        return gson.fromJson(json, type);
//    }
//
//    @Override
//    public <T> List<T> toJavaList(String json) {
//        return gson.fromJson(json, List.class);
//    }
//
//    @Override
//    public <T> List<T> toJavaList(String json, Type type) {
//        return gson.fromJson(json, TypeToken.getParameterized(List.class, type).getType());
//    }
//
//    @Override
//    public String toJson(Object obj) {
//        return gson.toJson(obj);
//    }
//}
