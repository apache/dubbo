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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.CALLBACK_INSTANCES_LIMIT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CALLBACK_INSTANCES;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_DESTROY_INVOKER;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_LOAD_MODEL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;
import static org.apache.dubbo.rpc.Constants.IS_SERVER_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.CALLBACK_SERVICE_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.CALLBACK_SERVICE_PROXY_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.CHANNEL_CALLBACK_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.Constants.IS_CALLBACK_SERVICE;

/**
 * callback service helper
 */
public class CallbackServiceCodec {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CallbackServiceCodec.class);

    private static final byte CALLBACK_NONE = 0x0;
    private static final byte CALLBACK_CREATE = 0x1;
    private static final byte CALLBACK_DESTROY = 0x2;
    private static final String INV_ATT_CALLBACK_KEY = "sys_callback_arg-";

    private final ProxyFactory proxyFactory;
    private final Protocol protocolSPI;
    private final FrameworkModel frameworkModel;
    private final DubboProtocol dubboProtocol;

    public CallbackServiceCodec(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        proxyFactory = frameworkModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        protocolSPI = frameworkModel.getExtensionLoader(Protocol.class).getExtension(DUBBO_PROTOCOL);
        dubboProtocol = (DubboProtocol) frameworkModel.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME, false);
    }

    private static byte isCallBack(URL url, String protocolServiceKey, String methodName, int argIndex) {
        // parameter callback rule: method-name.parameter-index(starting from 0).callback
        byte isCallback = CALLBACK_NONE;
        if (url != null && url.hasServiceMethodParameter(protocolServiceKey, methodName)) {
            String callback = url.getServiceParameter(protocolServiceKey, methodName + "." + argIndex + ".callback");
            if (callback != null) {
                if ("true".equalsIgnoreCase(callback)) {
                    isCallback = CALLBACK_CREATE;
                } else if ("false".equalsIgnoreCase(callback)) {
                    isCallback = CALLBACK_DESTROY;
                }
            }
        }
        return isCallback;
    }

    /**
     * export or unexport callback service on client side
     *
     * @param channel
     * @param url
     * @param clazz
     * @param inst
     * @param export
     * @throws IOException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String exportOrUnexportCallbackService(Channel channel, RpcInvocation inv, URL url, Class clazz, Object inst, Boolean export) throws IOException {
        int instid = System.identityHashCode(inst);

        Map<String, String> params = new HashMap<>(3);
        // no need to new client again
        params.put(IS_SERVER_KEY, Boolean.FALSE.toString());
        // mark it's a callback, for troubleshooting
        params.put(IS_CALLBACK_SERVICE, Boolean.TRUE.toString());
        String group = (inv == null ? null : (String) inv.getObjectAttachmentWithoutConvert(GROUP_KEY));
        if (group != null && group.length() > 0) {
            params.put(GROUP_KEY, group);
        }
        // add method, for verifying against method, automatic fallback (see dubbo protocol)
        params.put(METHODS_KEY, StringUtils.join(ClassUtils.getDeclaredMethodNames(clazz), ","));

        Map<String, String> tmpMap = new HashMap<>();
        if (url != null) {
            Map<String, String> parameters = url.getParameters();
            if (parameters != null && !parameters.isEmpty()) {
                tmpMap.putAll(parameters);
            }
        }
        tmpMap.putAll(params);

        tmpMap.remove(VERSION_KEY);// doesn't need to distinguish version for callback
        tmpMap.remove(Constants.BIND_PORT_KEY); //callback doesn't needs bind.port
        tmpMap.put(INTERFACE_KEY, clazz.getName());
        URL exportUrl = new ServiceConfigURL(DubboProtocol.NAME, channel.getLocalAddress().getAddress().getHostAddress(),
            channel.getLocalAddress().getPort(), clazz.getName() + "." + instid, tmpMap);

        // no need to generate multiple exporters for different channel in the same JVM, cache key cannot collide.
        String cacheKey = getClientSideCallbackServiceCacheKey(instid);
        String countKey = getClientSideCountKey(clazz.getName());
        if (export) {
            // one channel can have multiple callback instances, no need to re-export for different instance.
            if (!channel.hasAttribute(cacheKey)) {
                if (!isInstancesOverLimit(channel, url, clazz.getName(), instid, false)) {
                    ModuleModel moduleModel;
                    if (inv.getServiceModel() == null) {
                        //TODO should get scope model from url?
                        moduleModel = ApplicationModel.defaultModel().getDefaultModule();
                        logger.error(PROTOCOL_FAILED_LOAD_MODEL, "", "", "Unable to get Service Model from Invocation. Please check if your invocation failed! " +
                            "This error only happen in UT cases! Invocation:" + inv);
                    } else {
                        moduleModel = inv.getServiceModel().getModuleModel();
                    }

                    ServiceDescriptor serviceDescriptor = moduleModel.getServiceRepository().registerService(clazz);
                    ServiceMetadata serviceMetadata = new ServiceMetadata(clazz.getName() + "." + instid, exportUrl.getGroup(), exportUrl.getVersion(), clazz);
                    String serviceKey = BaseServiceMetadata.buildServiceKey(exportUrl.getPath(), group, exportUrl.getVersion());
                    ProviderModel providerModel = new ProviderModel(serviceKey, inst, serviceDescriptor, moduleModel, serviceMetadata, ClassUtils.getClassLoader(clazz));
                    moduleModel.getServiceRepository().registerProvider(providerModel);

                    exportUrl = exportUrl.setScopeModel(moduleModel);
                    exportUrl = exportUrl.setServiceModel(providerModel);
                    Invoker<?> invoker = proxyFactory.getInvoker(inst, clazz, exportUrl);
                    // should destroy resource?
                    Exporter<?> exporter = protocolSPI.export(invoker);
                    // this is used for tracing if instid has published service or not.
                    channel.setAttribute(cacheKey, exporter);
                    logger.info("Export a callback service :" + exportUrl + ", on " + channel + ", url is: " + url);
                    increaseInstanceCount(channel, countKey);
                }
            }
        } else {
            if (channel.hasAttribute(cacheKey)) {
                Exporter<?> exporter = (Exporter<?>) channel.getAttribute(cacheKey);
                exporter.unexport();
                channel.removeAttribute(cacheKey);
                decreaseInstanceCount(channel, countKey);
            }
        }
        return String.valueOf(instid);
    }

    /**
     * refer or destroy callback service on server side
     *
     * @param url
     */
    @SuppressWarnings("unchecked")
    private Object referOrDestroyCallbackService(Channel channel, URL url, Class<?> clazz, Invocation inv, int instid, boolean isRefer) {
        Object proxy;
        String invokerCacheKey = getServerSideCallbackInvokerCacheKey(channel, clazz.getName(), instid);
        String proxyCacheKey = getServerSideCallbackServiceCacheKey(channel, clazz.getName(), instid);
        proxy = channel.getAttribute(proxyCacheKey);
        String countkey = getServerSideCountKey(channel, clazz.getName());
        if (isRefer) {
            if (proxy == null) {
                URL referurl = URL.valueOf("callback://" + url.getAddress() + "/" + clazz.getName() + "?" + INTERFACE_KEY + "=" + clazz.getName());
                referurl = referurl.addParametersIfAbsent(url.getParameters()).removeParameter(METHODS_KEY);
                if (!isInstancesOverLimit(channel, referurl, clazz.getName(), instid, true)) {
                    url.getOrDefaultApplicationModel().getDefaultModule().getServiceRepository().registerService(clazz);
                    @SuppressWarnings("rawtypes")
                    Invoker<?> invoker = new ChannelWrappedInvoker(clazz, channel, referurl, String.valueOf(instid));
                    proxy = proxyFactory.getProxy(invoker);
                    channel.setAttribute(proxyCacheKey, proxy);
                    channel.setAttribute(invokerCacheKey, invoker);
                    increaseInstanceCount(channel, countkey);

                    //convert error fail fast .
                    //ignore concurrent problem.
                    Set<Invoker<?>> callbackInvokers = (Set<Invoker<?>>) channel.getAttribute(CHANNEL_CALLBACK_KEY);
                    if (callbackInvokers == null) {
                        callbackInvokers = new ConcurrentHashSet<>(1);
                        channel.setAttribute(CHANNEL_CALLBACK_KEY, callbackInvokers);
                    }
                    callbackInvokers.add(invoker);
                    logger.info("method " + inv.getMethodName() + " include a callback service :" + invoker.getUrl() + ", a proxy :" + invoker + " has been created.");
                }
            }
        } else {
            if (proxy != null) {
                Invoker<?> invoker = (Invoker<?>) channel.getAttribute(invokerCacheKey);
                try {
                    Set<Invoker<?>> callbackInvokers = (Set<Invoker<?>>) channel.getAttribute(CHANNEL_CALLBACK_KEY);
                    if (callbackInvokers != null) {
                        callbackInvokers.remove(invoker);
                    }
                    invoker.destroy();
                } catch (Exception e) {
                    logger.error(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", e.getMessage(), e);
                }
                // cancel refer, directly remove from the map
                channel.removeAttribute(proxyCacheKey);
                channel.removeAttribute(invokerCacheKey);
                decreaseInstanceCount(channel, countkey);
            }
        }
        return proxy;
    }

    private static String getClientSideCallbackServiceCacheKey(int instid) {
        return CALLBACK_SERVICE_KEY + "." + instid;
    }

    private static String getServerSideCallbackServiceCacheKey(Channel channel, String interfaceClass, int instid) {
        return CALLBACK_SERVICE_PROXY_KEY + "." + System.identityHashCode(channel) + "." + interfaceClass + "." + instid;
    }

    private static String getServerSideCallbackInvokerCacheKey(Channel channel, String interfaceClass, int instid) {
        return getServerSideCallbackServiceCacheKey(channel, interfaceClass, instid) + "." + "invoker";
    }

    private static String getClientSideCountKey(String interfaceClass) {
        return CALLBACK_SERVICE_KEY + "." + interfaceClass + ".COUNT";
    }

    private static String getServerSideCountKey(Channel channel, String interfaceClass) {
        return CALLBACK_SERVICE_PROXY_KEY + "." + System.identityHashCode(channel) + "." + interfaceClass + ".COUNT";
    }

    private static boolean isInstancesOverLimit(Channel channel, URL url, String interfaceClass, int instid, boolean isServer) {
        Integer count = (Integer) channel.getAttribute(isServer ? getServerSideCountKey(channel, interfaceClass) : getClientSideCountKey(interfaceClass));
        int limit = url.getParameter(CALLBACK_INSTANCES_LIMIT_KEY, DEFAULT_CALLBACK_INSTANCES);
        if (count != null && count >= limit) {
            //client side error
            throw new IllegalStateException("interface " + interfaceClass + " `s callback instances num exceed providers limit :" + limit
                + " ,current num: " + (count + 1) + ". The new callback service will not work !!! you can cancle the callback service which exported before. channel :" + channel);
        } else {
            return false;
        }
    }

    private static void increaseInstanceCount(Channel channel, String countkey) {
        try {
            //ignore concurrent problem?
            Integer count = (Integer) channel.getAttribute(countkey);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            channel.setAttribute(countkey, count);
        } catch (Exception e) {
            logger.error(COMMON_PROPERTY_TYPE_MISMATCH, "", "", e.getMessage(), e);
        }
    }

    private static void decreaseInstanceCount(Channel channel, String countkey) {
        try {
            Integer count = (Integer) channel.getAttribute(countkey);
            if (count == null || count <= 0) {
                return;
            } else {
                count--;
            }
            channel.setAttribute(countkey, count);
        } catch (Exception e) {
            logger.error(COMMON_PROPERTY_TYPE_MISMATCH, "", "", e.getMessage(), e);
        }
    }

    public Object encodeInvocationArgument(Channel channel, RpcInvocation inv, int paraIndex) throws IOException {
        // get URL directly
        URL url = inv.getInvoker() == null ? null : inv.getInvoker().getUrl();
        byte callbackStatus = isCallBack(url, inv.getProtocolServiceKey(), inv.getMethodName(), paraIndex);
        Object[] args = inv.getArguments();
        Class<?>[] pts = inv.getParameterTypes();
        switch (callbackStatus) {
            case CallbackServiceCodec.CALLBACK_CREATE:
                inv.setAttachment(INV_ATT_CALLBACK_KEY + paraIndex, exportOrUnexportCallbackService(channel, inv, url, pts[paraIndex], args[paraIndex], true));
                return null;
            case CallbackServiceCodec.CALLBACK_DESTROY:
                inv.setAttachment(INV_ATT_CALLBACK_KEY + paraIndex, exportOrUnexportCallbackService(channel, inv, url, pts[paraIndex], args[paraIndex], false));
                return null;
            default:
                return args[paraIndex];
        }
    }

    public Object decodeInvocationArgument(Channel channel, RpcInvocation inv, Class<?>[] pts, int paraIndex, Object inObject) throws IOException {
        // if it's a callback, create proxy on client side, callback interface on client side can be invoked through channel
        // need get URL from channel and env when decode
        URL url = null;
        try {
            url = dubboProtocol.getInvoker(channel, inv).getUrl();
        } catch (RemotingException e) {
            if (logger.isInfoEnabled()) {
                logger.info(e.getMessage(), e);
            }
            return inObject;
        }
        byte callbackstatus = isCallBack(url, inv.getProtocolServiceKey(), inv.getMethodName(), paraIndex);
        switch (callbackstatus) {
            case CallbackServiceCodec.CALLBACK_CREATE:
                try {
                    return referOrDestroyCallbackService(channel, url, pts[paraIndex], inv, Integer.parseInt(inv.getAttachment(INV_ATT_CALLBACK_KEY + paraIndex)), true);
                } catch (Exception e) {
                    logger.error(PROTOCOL_FAILED_DESTROY_INVOKER, "", "", e.getMessage(), e);
                    throw new IOException(StringUtils.toString(e));
                }
            case CallbackServiceCodec.CALLBACK_DESTROY:
                try {
                    return referOrDestroyCallbackService(channel, url, pts[paraIndex], inv, Integer.parseInt(inv.getAttachment(INV_ATT_CALLBACK_KEY + paraIndex)), false);
                } catch (Exception e) {
                    throw new IOException(StringUtils.toString(e));
                }
            default:
                return inObject;
        }
    }
}
