package org.apache.dubbo.common.json.impl;

import org.apache.dubbo.common.json.JSON;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FastJsonImpl implements JSON {

    @Override
    public <T> T toJavaObject(String json, Type type) {
        return com.alibaba.fastjson.JSON.parseObject(json, type);
    }

    @Override
    public <T> Set<T> toJavaSet(String json, Class<T> clazz) {
        JSONArray jsonArray = com.alibaba.fastjson.JSON.parseArray(json);
        Set<T> set = new HashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            set.add(jsonArray.getObject(i, clazz));
        }
        return set;
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> clazz) {
        return (List<T>) com.alibaba.fastjson.JSON.parseArray(json, new Type[]{clazz});
    }

    @Override
    public String toJson(Object obj) {
        return com.alibaba.fastjson.JSON.toJSONString(obj);
    }
}
