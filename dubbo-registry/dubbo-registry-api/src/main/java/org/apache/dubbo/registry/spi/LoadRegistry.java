package org.apache.dubbo.registry.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.*;
import static org.apache.dubbo.common.registry.Constants.SUBSCRIBE_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

public class LoadRegistry implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.loadRegistry;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * The spi method.
     *
     * @param params params
     * @return return value
     */
    @Override
    public Object invoke(Object... params) {
        return loadRegistries((AbstractInterfaceConfig) params[0], (Boolean) params[1]);
    }

    public static List<URL> loadRegistries(AbstractInterfaceConfig interfaceConfig, boolean provider) {
        // check && override if necessary
        List<URL> registryList = new ArrayList<>();
        ApplicationConfig application = interfaceConfig.getApplication();
        List<RegistryConfig> registries = interfaceConfig.getRegistries();
        if (CollectionUtils.isNotEmpty(registries)) {
            for (RegistryConfig config : registries) {
                // try to refresh registry in case it is set directly by user using config.setRegistries()
                if (!config.isRefreshed()) {
                    config.refresh();
                }
                String address = config.getAddress();
                if (StringUtils.isEmpty(address)) {
                    address = ANYHOST_VALUE;
                }
                if (!RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(address)) {
                    Map<String, String> map = new HashMap<String, String>();
                    AbstractConfig.appendParameters(map, application);
                    AbstractConfig.appendParameters(map, config);
                    map.put(PATH_KEY, RegistryService.class.getName());
                    AbstractInterfaceConfig.appendRuntimeParameters(map);
                    if (!map.containsKey(PROTOCOL_KEY)) {
                        map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                    }
                    List<URL> urls = UrlUtils.parseURLs(address, map);

                    for (URL url : urls) {
                        url = URLBuilder.from(url)
                            .addParameter(REGISTRY_KEY, url.getProtocol())
                            .setProtocol(extractRegistryType(url))
                            .setScopeModel(interfaceConfig.getScopeModel())
                            .build();
                        // provider delay register state will be checked in RegistryProtocol#export
                        if (provider || url.getParameter(SUBSCRIBE_KEY, true)) {
                            registryList.add(url);
                        }
                    }
                }
            }
        }
        return genCompatibleRegistries(interfaceConfig.getScopeModel(), registryList, provider);
    }

    private static List<URL> genCompatibleRegistries(ScopeModel scopeModel, List<URL> registryList, boolean provider) {
        List<URL> result = new ArrayList<>(registryList.size());
        registryList.forEach(registryURL -> {
            if (provider) {
                // for registries enabled service discovery, automatically register interface compatible addresses.
                String registerMode;
                if (SERVICE_REGISTRY_PROTOCOL.equals(registryURL.getProtocol())) {
                    registerMode = registryURL.getParameter(REGISTER_MODE_KEY, ConfigurationUtils.getCachedDynamicProperty(scopeModel, DUBBO_REGISTER_MODE_DEFAULT_KEY, DEFAULT_REGISTER_MODE_INSTANCE));
                    if (!isValidRegisterMode(registerMode)) {
                        registerMode = DEFAULT_REGISTER_MODE_INSTANCE;
                    }
                    result.add(registryURL);
                    if (DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode)
                        && registryNotExists(registryURL, registryList, REGISTRY_PROTOCOL)) {
                        URL interfaceCompatibleRegistryURL = URLBuilder.from(registryURL)
                            .setProtocol(REGISTRY_PROTOCOL)
                            .removeParameter(REGISTRY_TYPE_KEY)
                            .build();
                        result.add(interfaceCompatibleRegistryURL);
                    }
                } else {
                    registerMode = registryURL.getParameter(REGISTER_MODE_KEY, ConfigurationUtils.getCachedDynamicProperty(scopeModel, DUBBO_REGISTER_MODE_DEFAULT_KEY, DEFAULT_REGISTER_MODE_ALL));
                    if (!isValidRegisterMode(registerMode)) {
                        registerMode = DEFAULT_REGISTER_MODE_INTERFACE;
                    }
                    if ((DEFAULT_REGISTER_MODE_INSTANCE.equalsIgnoreCase(registerMode) || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode))
                        && registryNotExists(registryURL, registryList, SERVICE_REGISTRY_PROTOCOL)) {
                        URL serviceDiscoveryRegistryURL = URLBuilder.from(registryURL)
                            .setProtocol(SERVICE_REGISTRY_PROTOCOL)
                            .removeParameter(REGISTRY_TYPE_KEY)
                            .build();
                        result.add(serviceDiscoveryRegistryURL);
                    }

                    if (DEFAULT_REGISTER_MODE_INTERFACE.equalsIgnoreCase(registerMode) || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(registerMode)) {
                        result.add(registryURL);
                    }
                }

                FrameworkStatusReportService reportService = ScopeModelUtil.getApplicationModel(scopeModel).getBeanFactory().getBean(FrameworkStatusReportService.class);
                reportService.reportRegistrationStatus(reportService.createRegistrationReport(registerMode));
            } else {
                result.add(registryURL);
            }
        });

        return result;
    }

    private static boolean isValidRegisterMode(String mode) {
        return isNotEmpty(mode)
            && (DEFAULT_REGISTER_MODE_INTERFACE.equalsIgnoreCase(mode)
            || DEFAULT_REGISTER_MODE_INSTANCE.equalsIgnoreCase(mode)
            || DEFAULT_REGISTER_MODE_ALL.equalsIgnoreCase(mode)
        );
    }

    private static String extractRegistryType(URL url) {
        return UrlUtils.hasServiceDiscoveryRegistryTypeKey(url) ? SERVICE_REGISTRY_PROTOCOL : getRegistryProtocolType(url);
    }

    private static String getRegistryProtocolType(URL url) {
        String registryProtocol = url.getParameter("registry-protocol-type");
        return isNotEmpty(registryProtocol) ? registryProtocol : REGISTRY_PROTOCOL;
    }

    private static boolean registryNotExists(URL registryURL, List<URL> registryList, String registryType) {
        return registryList.stream().noneMatch(
            url -> registryType.equals(url.getProtocol()) && registryURL.getBackupAddress().equals(url.getBackupAddress())
        );
    }

}
