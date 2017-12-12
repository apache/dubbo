package com.alibaba.dubbo.qos.command.CommandImpl;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.model.ApplicationModel;
import com.alibaba.dubbo.config.model.ProviderModel;
import com.alibaba.dubbo.qos.command.BaseCommand;
import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.annotation.Cmd;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.support.ProviderConsumerRegTable;
import com.alibaba.dubbo.registry.support.ProviderInvokerWrapper;

import java.util.List;
import java.util.Set;

/**
 * @author qinliujie
 * @date 2017/11/21
 */
@Cmd(name = "online", summary = "online dubbo", example = {
        "online dubbo",
        "online xx.xx.xxx.service"
})
public class Online implements BaseCommand {
    private Logger logger = LoggerFactory.getLogger(Online.class);
    private RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        logger.info("receive online command");
        String servicePattern = ".*";
        if (args != null && args.length > 0) {
            servicePattern = args[0];
        }

        boolean hasService = false;

        List<ProviderModel> providerModelList = ApplicationModel.allProviderModels();
        for (ProviderModel providerModel : providerModelList) {
            if (providerModel.getServiceName().matches(servicePattern)) {
                hasService = true;
                Set<ProviderInvokerWrapper> providerInvokerWrapperSet = ProviderConsumerRegTable.getProviderInvoker(providerModel.getServiceName());
                for (ProviderInvokerWrapper providerInvokerWrapper : providerInvokerWrapperSet) {
                    if (providerInvokerWrapper.isReg()) {
                        continue;
                    }
                    Registry registry = registryFactory.getRegistry(providerInvokerWrapper.getRegistryUrl());
                    registry.register(providerInvokerWrapper.getProviderUrl());
                    providerInvokerWrapper.setReg(true);
                }
            }
        }

        if (hasService) {
            return "OK";
        } else {
            return "service not found";
        }

    }
}
