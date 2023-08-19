package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.util.ConfigValidationUtils;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.Dispatcher;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.Exchanger;
import org.apache.dubbo.remoting.telnet.TelnetHandler;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.CLIENT_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.CODEC_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.DISPATCHER_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.EXCHANGER_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.SERIALIZATION_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.SERVER_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.TELNET_KEY;
import static org.apache.dubbo.remoting.RemotingConstants.TRANSPORTER_KEY;

@Activate
public class ProtocolConfigValidator implements ConfigValidator<ProtocolConfig> {

    @Override
    public void validate(ProtocolConfig config) {
        validateProtocolConfig(config);
    }

    public static void validateProtocolConfig(ProtocolConfig config) {
        if (config != null) {
            String name = config.getName();
            ConfigValidationUtils.checkName("name", name);
            ConfigValidationUtils.checkHost(HOST_KEY, config.getHost());
            ConfigValidationUtils.checkPathName("contextpath", config.getContextpath());


            if (DUBBO_PROTOCOL.equals(name)) {
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Codec2.class, CODEC_KEY, config.getCodec());
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Serialization.class, SERIALIZATION_KEY, config.getSerialization());
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Transporter.class, SERVER_KEY, config.getServer());
                ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Transporter.class, CLIENT_KEY, config.getClient());
            }

            ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), TelnetHandler.class, TELNET_KEY, config.getTelnet());
            ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), StatusChecker.class, "status", config.getStatus());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Transporter.class, TRANSPORTER_KEY, config.getTransporter());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Exchanger.class, EXCHANGER_KEY, config.getExchanger());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Dispatcher.class, DISPATCHER_KEY, config.getDispatcher());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), Dispatcher.class, "dispather", config.getDispather());
            ConfigValidationUtils.checkExtension(config.getScopeModel(), ThreadPool.class, THREADPOOL_KEY, config.getThreadpool());
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ProtocolConfig.class.equals(configClass);
    }
}
