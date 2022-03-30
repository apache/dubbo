package org.apache.dubbo.common.convert;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConverterUtil {
    private final FrameworkModel frameworkModel;
    private final Map<Class<?>, Map<Class<?>, List<Converter>>> converterCache = new ConcurrentHashMap<>();

    public ConverterUtil(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    /**
     * Get the Converter instance from {@link ExtensionLoader} with the specified source and target type
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @return
     * @see ExtensionLoader#getSupportedExtensionInstances()
     */
    public Converter<?, ?> getConverter(Class<?> sourceType, Class<?> targetType) {
        Map<Class<?>, List<Converter>> toTargetMap = converterCache.computeIfAbsent(sourceType, (k) -> new ConcurrentHashMap<>());
        List<Converter> converters = toTargetMap.computeIfAbsent(targetType, (k) -> frameworkModel.getExtensionLoader(Converter.class)
            .getSupportedExtensionInstances()
            .stream()
            .filter(converter -> converter.accept(sourceType, targetType))
            .collect(Collectors.toList()));

        return converters.size() > 0 ? converters.get(0) : null;
    }

    /**
     * Convert the value of source to target-type value if possible
     *
     * @param source     the value of source
     * @param targetType the target type
     * @param <T>        the target type
     * @return <code>null</code> if can't be converted
     * @since 2.7.8
     */
    public <T> T convertIfPossible(Object source, Class<T> targetType) {
        Converter converter = getConverter(source.getClass(), targetType);
        if (converter != null) {
            return (T) converter.convert(source);
        }
        return null;
    }
}
