package org.apache.dubbo.common.config;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class OrderedPropertiesConfiguration implements Configuration{
    private Properties properties = new Properties();
    private ModuleModel moduleModel;

    public OrderedPropertiesConfiguration(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
    }

    public void refresh() {
        ExtensionLoader<OrderedPropertiesProvider> propertiesProviderExtensionLoader = moduleModel.getExtensionLoader(OrderedPropertiesProvider.class);
        Set<String> propertiesProviderNames = propertiesProviderExtensionLoader.getSupportedExtensions();
        if (propertiesProviderNames == null || propertiesProviderNames.isEmpty()) {
            return;
        }
        List<OrderedPropertiesProvider> orderedPropertiesProviders = new ArrayList<>();
        for (String propertiesProviderName : propertiesProviderNames) {
            orderedPropertiesProviders.add(propertiesProviderExtensionLoader.getExtension(propertiesProviderName));
        }

        //order the propertiesProvider according the priority descending
        orderedPropertiesProviders.sort((OrderedPropertiesProvider a, OrderedPropertiesProvider b) -> {
            return b.priority() - a.priority();
        });


        //override the properties.
        for (OrderedPropertiesProvider orderedPropertiesProvider :
            orderedPropertiesProviders) {
            properties.putAll(orderedPropertiesProvider.initProperties());
        }

    }

    @Override
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    @Override
    public Object getInternalProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public String remove(String key) {
        return (String) properties.remove(key);
    }

    @Deprecated
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return (Map) properties;
    }
}
