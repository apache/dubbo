package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptySet;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;

public class ServiceNameMappingUtils {

    public static String buildMappingKey(URL url) {
        return buildGroup(url.getServiceInterface());
    }

    public static String buildGroup(String serviceInterface) {
        //the issue : https://github.com/apache/dubbo/issues/4671
//        return DEFAULT_MAPPING_GROUP + SLASH + serviceInterface;
        return serviceInterface;
    }

    public static String toStringKeys(Set<String> serviceNames) {
        if (CollectionUtils.isEmpty(serviceNames)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String n : serviceNames) {
            builder.append(n);
            builder.append(COMMA_SEPARATOR);
        }

        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public static Set<String> getAppNames(String content) {
        if (StringUtils.isBlank(content)) {
            return emptySet();
        }
        return new TreeSet<>(Arrays.asList(content.split(COMMA_SEPARATOR)));
    }

    public static Set<String> getMappingByUrl(URL consumerURL) {
        String providedBy = consumerURL.getParameter(RegistryConstants.PROVIDED_BY);
        if(StringUtils.isBlank(providedBy)) {
            return null;
        }
        return AbstractServiceNameMapping.parseServices(providedBy);
    }
}
