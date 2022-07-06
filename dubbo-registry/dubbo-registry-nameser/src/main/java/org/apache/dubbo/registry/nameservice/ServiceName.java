package org.apache.dubbo.registry.nameservice;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Objects;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

public class ServiceName {
	
	public static final String DEFAULT_PARAM_VALUE = "";
	
    public static final String NAME_SEPARATOR = ":";

    public static final String VALUE_SEPARATOR = ",";

    public static final String WILDCARD = "*";


    private String category;

    private String serviceInterface;

    private String version;

    private String group;

    private String value;
    
    private String groupModel;

    public ServiceName() {
    }

    public ServiceName(URL url,String groupModel) {
        this.serviceInterface = url.getParameter(INTERFACE_KEY);
        this.category = isConcrete(serviceInterface) ? DEFAULT_CATEGORY : url.getParameter(CATEGORY_KEY);
        this.version = url.getParameter(VERSION_KEY, DEFAULT_PARAM_VALUE);
        this.group = url.getParameter(GROUP_KEY, DEFAULT_PARAM_VALUE);
        this.groupModel = groupModel;
        this.value = toValue();
    }
    
    public boolean isConcrete() {
        return isConcrete(serviceInterface) && isConcrete(version) && isConcrete(group);
    }

    public boolean isCompatible(ServiceName serviceName) {

        // The argument must be the concrete NacosServiceName
        if (!serviceName.isConcrete()) {
            return false;
        }

        // Not match comparison
        if (!StringUtils.isEquals(this.category, serviceName.category)
                && !matchRange(this.category, serviceName.category)) {
            return false;
        }

        if (!StringUtils.isEquals(this.serviceInterface, serviceName.serviceInterface)) {
            return false;
        }

        // wildcard condition
        if (isWildcard(this.version)) {
            return true;
        }

        if (isWildcard(this.group)) {
            return true;
        }

        // range condition
        if (!StringUtils.isEquals(this.version, serviceName.version)
                && !matchRange(this.version, serviceName.version)) {
            return false;
        }

        if (!StringUtils.isEquals(this.group, serviceName.group) &&
                !matchRange(this.group, serviceName.group)) {
            return false;
        }

        return true;
    }

    private boolean matchRange(String range, String value) {
        if (isBlank(range)) {
            return true;
        }
        if (!isRange(range)) {
            return false;
        }
        String[] values = range.split(VALUE_SEPARATOR);
        return Arrays.asList(values).contains(value);
    }

    private boolean isConcrete(String value) {
        return !isWildcard(value) && !isRange(value);
    }

    private boolean isWildcard(String value) {
        return WILDCARD.equals(value);
    }

    private boolean isRange(String value) {
        return value != null && value.contains(VALUE_SEPARATOR) && value.split(VALUE_SEPARATOR).length > 1;
    }

    private String toValue() {
    	
    	
    	
    	if(Objects.equals(this.group, "topic")) {
    		return category +
                    NAME_SEPARATOR + serviceInterface +
                    NAME_SEPARATOR + version +
                    NAME_SEPARATOR + group;
    	}
        return category + NAME_SEPARATOR + serviceInterface;
    }
    
    
    public String getValue() {
    	return value;
    }
    
    public String getServiceInterface() {
    	return this.getServiceInterface();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceName)) {
            return false;
        }
        ServiceName that = (ServiceName) o;
        return Objects.equals(getValue(), that.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public String toString() {
        return getValue();
    }
}
