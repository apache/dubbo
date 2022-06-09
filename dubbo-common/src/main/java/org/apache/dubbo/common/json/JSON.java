package org.apache.dubbo.common.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public interface JSON {

    <T> T toJavaObject(String json, Type type);

    <T> List<T> toJavaList(String json, Class<T> clazz);

    <T> Set<T> toJavaSet(String json, Class<T> clazz);

    String toJson(Object obj);
}
