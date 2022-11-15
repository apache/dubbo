package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;

public class NacosConnectionManager {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosNamingServiceUtils.class);


    private final URL connectionURL;

    private final List<NamingService> namingServiceList = new LinkedList<>();

    public NacosConnectionManager(URL connectionURL) {
        this.connectionURL = connectionURL;
        // create default one
        this.namingServiceList.add(createNamingService());
    }

    /**
     * for ut only
     */
    @Deprecated
    protected NacosConnectionManager(NamingService namingService) {
        this.connectionURL = null;
        // create default one
        this.namingServiceList.add(namingService);
    }

    public synchronized NamingService getNamingService() {
        return namingServiceList.get(ThreadLocalRandom.current().nextInt(namingServiceList.size()));
    }

    public synchronized NamingService getNamingService(Set<NamingService> selected) {
        List<NamingService> copyOfNamingService = new LinkedList<>(namingServiceList);
        copyOfNamingService.removeAll(selected);
        if (copyOfNamingService.size() == 0) {
            this.namingServiceList.add(createNamingService());
            return getNamingService(selected);
        }
        return copyOfNamingService.get(ThreadLocalRandom.current().nextInt(namingServiceList.size()));
    }

    public synchronized void shutdownAll() {
        for (NamingService namingService : namingServiceList) {
            try {
                namingService.shutDown();
            } catch (Exception e) {
                logger.warn(REGISTRY_NACOS_EXCEPTION, "", "", "Unable to shutdown nacos naming service", e);
            }
        }
    }

    /**
     * Create an instance of {@link NamingService} from specified {@link URL connection url}
     *
     * @return {@link NamingService}
     */
    private NamingService createNamingService() {
        Properties nacosProperties = buildNacosProperties(this.connectionURL);
        NamingService namingService;
        try {
            namingService = NacosFactory.createNamingService(nacosProperties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(REGISTRY_NACOS_EXCEPTION, "", "", e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return namingService;
    }

    private Properties buildNacosProperties(URL url) {
        Properties properties = new Properties();
        setServerAddr(url, properties);
        setProperties(url, properties);
        return properties;
    }

    private void setServerAddr(URL url, Properties properties) {
        StringBuilder serverAddrBuilder =
            new StringBuilder(url.getHost()) // Host
                .append(':')
                .append(url.getPort()); // Port

        // Append backup parameter as other servers
        String backup = url.getParameter(BACKUP_KEY);
        if (StringUtils.isNotEmpty(backup)) {
            serverAddrBuilder.append(',').append(backup);
        }

        String serverAddr = serverAddrBuilder.toString();
        properties.put(SERVER_ADDR, serverAddr);
    }

    private void setProperties(URL url, Properties properties) {
        putPropertyIfAbsent(url, properties, NACOS_NAMING_LOG_NAME, null);

        // @since 2.7.8 : Refactoring
        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);
        if (StringUtils.isNotEmpty(url.getUsername())) {
            properties.put(USERNAME, url.getUsername());
        }
        if (StringUtils.isNotEmpty(url.getPassword())) {
            properties.put(PASSWORD, url.getPassword());
        }

        putPropertyIfAbsent(url, properties, NAMING_LOAD_CACHE_AT_START, "true");
    }

    private void putPropertyIfAbsent(URL url, Properties properties, String propertyName, String defaultValue) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        } else {
            // when defaultValue is empty, we should not set empty value
            if (StringUtils.isNotEmpty(defaultValue)) {
                properties.setProperty(propertyName, defaultValue);
            }
        }
    }
}
