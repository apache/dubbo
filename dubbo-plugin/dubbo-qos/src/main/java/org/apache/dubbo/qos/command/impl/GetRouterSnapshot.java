package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.annotation.Cmd;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.migration.MigrationInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.directory.AbstractDirectory;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import java.util.Map;

@Cmd(name = "getRouterSnapshot", summary = "Get State Router Snapshot.", example = "getRouterSnapshot xx.xx.xxx.service")
public class GetRouterSnapshot implements BaseCommand {
    private FrameworkModel frameworkModel;

    public GetRouterSnapshot(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        if (args.length != 1) {
            return "args count should be 1. example getRouterSnapshot xx.xx.xxx.service";
        }
        String servicePattern = args[0];
        StringBuilder stringBuilder = new StringBuilder();
        for (ConsumerModel consumerModel : frameworkModel.getServiceRepository().allConsumerModels()) {
            try {
                ServiceMetadata metadata = consumerModel.getServiceMetadata();
                if (metadata.getServiceKey().matches(servicePattern) || metadata.getDisplayServiceKey().matches(servicePattern)) {
                    Object object = metadata.getAttribute(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY);
                    Map<Registry, MigrationInvoker<?>> invokerMap;
                    if (object instanceof Map) {
                        invokerMap = (Map<Registry, MigrationInvoker<?>>) object;
                        for (Map.Entry<Registry, MigrationInvoker<?>> invokerEntry : invokerMap.entrySet()) {
                            Directory<?> directory = invokerEntry.getValue().getDirectory();
                            StateRouter<?> headStateRouter = directory.getRouterChain().getHeadStateRouter();
                            stringBuilder.append(metadata.getServiceKey()).append("@").append(Integer.toHexString(System.identityHashCode(metadata)))
                                .append("\n")
                                .append("[ All Invokers:").append(directory.getAllInvokers().size()).append(" ] ")
                                .append("[ Valid Invokers: ").append(((AbstractDirectory<?>)directory).getValidInvokers().size()).append(" ]\n")
                                .append("\n")
                                .append(headStateRouter.buildSnapshot())
                                .append("\n\n");
                        }
                    }
                }
            } catch (Throwable ignore) {

            }
        }
        return stringBuilder.toString();
    }
}
