package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author cvictory ON 2018/9/18
 */
public class DefaultTypeBuilder {

    public static TypeDescriptor build(Class<?> clazz, Map<Class<?>, TypeDescriptor> typeCache) {
//        final String canonicalName = clazz.getCanonicalName();
        final String name = clazz.getName();

        TypeDescriptor td = new TypeDescriptor(name);
        // Try to get a cached definition
        if (typeCache.containsKey(clazz)) {
            return typeCache.get(clazz);
        }

        // Primitive type
        if (!needAnalyzing(clazz)) {
            return td;
        }

        // Custom type
        td.setCustom(true);

        List<Field> fields = getNonStaticFields(clazz);
        for (Field field : fields) {
            String fieldName = field.getName();
            Class<?> fieldClass = field.getType();
            Type fieldType = field.getGenericType();

            TypeDescriptor fieldTd = TypeDescriptorBuilder.build(fieldType, fieldClass, typeCache);
            // if custom, renew and remove properties.
            if(fieldTd.isCustom()){
                fieldTd = TypeDescriptor.simplifyTypeDescriptor(fieldTd);
            }
            td.getProperties().put(fieldName, fieldTd);
        }

        typeCache.put(clazz, td);
        return td;
    }

    private static List<Field> getNonStaticFields(final Class<?> clazz) {
        List<Field> result = new ArrayList<Field>();
        Class<?> target = clazz;
        while (target != null) {

            Field[] fields = target.getDeclaredFields();
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }

                result.add(field);
            }
            target = target.getSuperclass();
        }

        return result;
    }


    private static Set<String> closedTypes = new HashSet<String>(Arrays.asList("boolean", "byte", "char", "double", "float", "int", "long", "short", "void"));


    /**
     * <pre>
     * 是否需要分析参数 clazz ：
     * clazz 是否为 primitive 类型
     *    若为 primitive 类型，则不分析
     * </pre>
     *
     * @param clazz
     * @return
     */
    private static boolean needAnalyzing(Class<?> clazz) {
        String canonicalName = clazz.getCanonicalName();

        if (closedTypes != null && closedTypes.size() > 0) {
            for (String type : closedTypes) {
                if (canonicalName.startsWith(type)) {
                    return false;
                }
            }
        }
        if (canonicalName.equals("java.lang.String")) {
            return false;
        }
        // bootstrap classloader will be ignored.
        if (clazz.getClassLoader() == null) {
            return false;
        }

        return true;
    }

    private DefaultTypeBuilder() {
    }
}
