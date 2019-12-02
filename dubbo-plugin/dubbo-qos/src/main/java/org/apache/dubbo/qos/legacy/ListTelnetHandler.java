/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.qos.legacy;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.command.util.ServiceCheckUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceRepository;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ListTelnetHandler handler list services and its methods details.
 */
@Activate
@Help(parameter = "[-l] [service]", summary = "List services and methods.", detail = "List services and methods.")
public class ListTelnetHandler implements TelnetHandler {

    private ServiceRepository serviceRepository = ApplicationModel.getServiceRepository();

    @Override
    public String telnet(Channel channel, String message) {
        StringBuilder buf = new StringBuilder();
        String service = null;
        boolean detail = false;
        if (message.length() > 0) {
            String[] parts = message.split("\\s+");
            for (String part : parts) {
                if ("-l".equals(part)) {
                    detail = true;
                } else {
                    if (!StringUtils.isEmpty(service)) {
                        return "Invalid parameter " + part;
                    }
                    service = part;
                }
            }
        } else {
            service = (String) channel.getAttribute(ChangeTelnetHandler.SERVICE_KEY);
            if (StringUtils.isNotEmpty(service)) {
                buf.append("Use default service ").append(service).append(".\r\n");
            }
        }

        if (StringUtils.isEmpty(service)) {
            printAllServices(buf, detail);
        } else {
            printSpecifiedService(service, buf, detail);

            if (buf.length() == 0) {
                buf.append("No such service: ").append(service);
            }
        }
        return buf.toString();
    }

    private void printAllServices(StringBuilder buf, boolean detail) {
        printAllProvidedServices(buf, detail);
        printAllReferredServices(buf, detail);
    }

    private void printAllProvidedServices(StringBuilder buf, boolean detail) {
        List<ProviderModel> providerModels = serviceRepository.getExportedServices();
        if (!providerModels.isEmpty()) {
            buf.append("PROVIDER:\r\n");
        }

        for (ProviderModel provider : providerModels) {
            buf.append(provider.getServiceKey());
            if (detail) {
                buf.append(" -> ");
                buf.append(" published: ");
                buf.append(ServiceCheckUtils.isRegistered(provider) ? "Y" : "N");
            }
            buf.append("\r\n");
        }
    }

    private void printAllReferredServices(StringBuilder buf, boolean detail) {
        List<ConsumerModel> consumerModels = serviceRepository.getReferredServices();
        if (!consumerModels.isEmpty()) {
            buf.append("CONSUMER:\r\n");
        }

        for (ConsumerModel consumer : consumerModels) {
            buf.append(consumer.getServiceKey());
            if (detail) {
                buf.append(" -> ");
                buf.append(" addresses: ");
                buf.append(ServiceCheckUtils.getConsumerAddressNum(consumer));
            }
        }
    }

    private void printSpecifiedService(String service, StringBuilder buf, boolean detail) {
        printSpecifiedProvidedService(service, buf, detail);
        printSpecifiedReferredService(service, buf, detail);
    }

    private void printSpecifiedProvidedService(String service, StringBuilder buf, boolean detail) {
        for (ProviderModel provider : ApplicationModel.allProviderModels()) {
            if (isProviderMatched(service,provider)) {
                buf.append(provider.getServiceKey()).append(" (as provider):\r\n");
                for (MethodDescriptor method : provider.getAllMethods()) {
                    printMethod(method.getMethod(), buf, detail);
                }
            }
        }
    }

    private void printSpecifiedReferredService(String service, StringBuilder buf, boolean detail) {
        for (ConsumerModel consumer : ApplicationModel.allConsumerModels()) {
            if (isConsumerMatcher(service,consumer)) {
                buf.append(consumer.getServiceKey()).append(" (as consumer):\r\n");
                for (MethodDescriptor method : consumer.getAllMethods()) {
                    printMethod(method.getMethod(), buf, detail);
                }
            }
        }
    }

    private void printMethod(Method method, StringBuilder buf, boolean detail) {
        if (detail) {
            buf.append('\t').append(ReflectUtils.getName(method));
        } else {
            buf.append('\t').append(method.getName());
        }
        buf.append("\r\n");
    }

    private boolean isProviderMatched(String service, ProviderModel provider) {
        return service.equalsIgnoreCase(provider.getServiceKey())
                || service.equalsIgnoreCase(provider.getServiceInterfaceClass().getName())
                || service.equalsIgnoreCase(provider.getServiceInterfaceClass().getSimpleName());
    }

    private boolean isConsumerMatcher(String service,ConsumerModel consumer) {
        return service.equalsIgnoreCase(consumer.getServiceKey())
                || service.equalsIgnoreCase(consumer.getServiceInterfaceClass().getName())
                || service.equalsIgnoreCase(consumer.getServiceInterfaceClass().getSimpleName());
    }
}
