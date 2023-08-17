package org.apache.dubbo.config.utils.validator;

import org.apache.dubbo.common.config.PropertiesConfiguration;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.utils.ConfigValidationUtils;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_SECONDS_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_CLASS_NOT_FOUND;
import static org.apache.dubbo.config.Constants.ARCHITECTURE;
import static org.apache.dubbo.config.Constants.ENVIRONMENT;
import static org.apache.dubbo.config.Constants.NAME;
import static org.apache.dubbo.config.Constants.ORGANIZATION;
import static org.apache.dubbo.config.Constants.OWNER;

@Activate
public class ApplicationConfigValidator implements ConfigValidator<ApplicationConfig> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ApplicationConfigValidator.class);

    public static void validateApplicationConfig(ApplicationConfig config) {
        if (config == null) {
            return;
        }

        if (!config.isValid()) {
            throw new IllegalStateException("No application config found or it's not a valid config! " +
                "Please add <dubbo:application name=\"...\" /> to your spring config.");
        }

        // backward compatibility
        ScopeModel scopeModel = ScopeModelUtil.getOrDefaultApplicationModel(config.getScopeModel());
        PropertiesConfiguration configuration = scopeModel.modelEnvironment().getPropertiesConfiguration();
        String wait = configuration.getProperty(SHUTDOWN_WAIT_KEY);
        if (wait != null && wait.trim().length() > 0) {
            System.setProperty(SHUTDOWN_WAIT_KEY, wait.trim());
        } else {
            wait = configuration.getProperty(SHUTDOWN_WAIT_SECONDS_KEY);
            if (wait != null && wait.trim().length() > 0) {
                System.setProperty(SHUTDOWN_WAIT_SECONDS_KEY, wait.trim());
            }
        }

        ConfigValidationUtils.checkName(NAME, config.getName());
        ConfigValidationUtils.checkMultiName(OWNER, config.getOwner());
        ConfigValidationUtils.checkName(ORGANIZATION, config.getOrganization());
        ConfigValidationUtils.checkName(ARCHITECTURE, config.getArchitecture());
        ConfigValidationUtils.checkName(ENVIRONMENT, config.getEnvironment());
        ConfigValidationUtils.checkParameterName(config.getParameters());
        checkQosDependency(config);
    }

    public static void checkQosDependency(ApplicationConfig config) {
        if (!Boolean.FALSE.equals(config.getQosEnable())) {
            try {
                ClassUtils.forName("org.apache.dubbo.qos.protocol.QosProtocolWrapper");
            } catch (ClassNotFoundException e) {
                logger.warn(COMMON_CLASS_NOT_FOUND, "", "", "No QosProtocolWrapper class was found. Please check the dependency of dubbo-qos whether was imported correctly.", e);
            }
        }
    }

    @Override
    public void validate(ApplicationConfig config) {
        validateApplicationConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ApplicationConfig.class.isAssignableFrom(configClass);
    }
}
