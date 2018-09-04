package org.apache.dubbo.servicedata.integration;


import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.servicedata.ServiceStoreFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.dubbo.common.Constants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.Constants.QOS_ENABLE;
import static org.apache.dubbo.common.Constants.QOS_PORT;
import static org.apache.dubbo.common.Constants.VALIDATION_KEY;

/**
 * @author cvictory ON 2018/8/24
 * for backup , have removed.
 */
public class ServiceStoreProtocol {
//    implements
// Protocol {
//
//
//    private ServiceStoreFactory serviceStoreFactory = ExtensionLoader.getExtensionLoader(ServiceStoreFactory.class).getAdaptiveExtension();;
//
//    public ServiceStoreProtocol() {
//    }
//
//    public int getDefaultPort() {
//        return 9099;
//    }
//
//    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
////        //export invoker
////        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker);
////
////        URL registryUrl = getRegistryUrl(originInvoker);
////
////        //registry provider
////        final Registry registry = getRegistry(originInvoker);
//        final URL serviceStoreProviderUrl = getServiceStoreProviderUrl(invoker);
//
//        serviceStoreFactory.getServiceStore(getServiceStoreUrl(invoker)).put(serviceStoreProviderUrl);
//        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(serviceStoreProviderUrl);
//        return new DestroyableExporter(this, invoker, overrideSubscribeUrl, serviceStoreProviderUrl);
//    }
//
//    private URL getServiceStoreUrl(Invoker<?> originInvoker) {
//        URL servicestoreUrl = originInvoker.getUrl();
//        if (Constants.SERVICE_STORE_KEY.equals(servicestoreUrl.getProtocol())) {
//            String protocol = servicestoreUrl.getParameter(Constants.SERVICE_STORE_KEY, Constants.DEFAULT_DIRECTORY);
//            servicestoreUrl = servicestoreUrl.setProtocol(protocol).removeParameter(Constants.SERVICE_STORE_KEY);
//        }
//        return servicestoreUrl;
//    }
//
//    private URL getServiceStoreUrl(URL servicestoreUrl){
//        if (Constants.SERVICE_STORE_KEY.equals(servicestoreUrl.getProtocol())) {
//            String protocol = servicestoreUrl.getParameter(Constants.SERVICE_STORE_KEY, Constants.DEFAULT_DIRECTORY);
//            servicestoreUrl = servicestoreUrl.setProtocol(protocol).removeParameter(Constants.SERVICE_STORE_KEY);
//        }
//        return servicestoreUrl;
//    }
//
//    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
//        final URL serviceStoreProviderUrl = getServiceStoreTargetUrl(url);
//
//        serviceStoreFactory.getServiceStore(getServiceStoreUrl(url)).put(serviceStoreProviderUrl);
//        return null;
//    }
//
//    public void destroy() {
//
//    }
//
//
//    private ServiceStore getServiceStore(final Invoker<?> originInvoker) {
//        URL registryUrl = getServiceStoreUrl(originInvoker);
//        return serviceStoreFactory.getServiceStore(registryUrl);
//    }
//
//    /**
//     * Return the url that is registered to the registry and filter the url parameter once
//     *
//     * @param originInvoker
//     * @return
//     */
//    private URL getServiceStoreProviderUrl(final Invoker<?> originInvoker) {
//        URL providerUrl = getServiceStoreTargetUrl(originInvoker);
//        //The address you see at the registry
//        final URL registedProviderUrl = providerUrl.removeParameters(getFilteredKeys(providerUrl))
//                .removeParameter(Constants.MONITOR_KEY)
//                .removeParameter(Constants.BIND_IP_KEY)
//                .removeParameter(Constants.BIND_PORT_KEY)
//                .removeParameter(QOS_ENABLE)
//                .removeParameter(QOS_PORT)
//                .removeParameter(ACCEPT_FOREIGN_IP)
//                .removeParameter(VALIDATION_KEY);
//        return registedProviderUrl;
//    }
//
//    private URL getSubscribedOverrideUrl(URL registedProviderUrl) {
//        return registedProviderUrl.setProtocol(Constants.PROVIDER_PROTOCOL)
//                .addParameters(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY,
//                        Constants.CHECK_KEY, String.valueOf(false));
//    }
//
//    /**
//     * Get the address of the providerUrl through the url of the invoker
//     *
//     * @param origininvoker
//     * @return
//     */
//    private URL getServiceStoreTargetUrl(final Invoker<?> origininvoker) {
//        String export = origininvoker.getUrl().getParameterAndDecoded(Constants.EXPORT_KEY);
//        if (export == null || export.length() == 0) {
//            throw new IllegalArgumentException("The registry export url is null! registry: " + origininvoker.getUrl());
//        }
//
//        URL providerUrl = URL.valueOf(export);
//        return providerUrl;
//    }
//
//    private URL getServiceStoreTargetUrl(final URL url) {
//        String export = url.getParameterAndDecoded(Constants.EXPORT_KEY);
//        if (export == null || export.length() == 0) {
//            throw new IllegalArgumentException("The registry export url is null! registry: " + url);
//        }
//
//        URL providerUrl = URL.valueOf(export);
//        return providerUrl;
//    }
//
//
//    //Filter the parameters that do not need to be output in url(Starting with .)
//    private static String[] getFilteredKeys(URL url) {
//        Map<String, String> params = url.getParameters();
//        if (params != null && !params.isEmpty()) {
//            List<String> filteredKeys = new ArrayList<String>();
//            for (Map.Entry<String, String> entry : params.entrySet()) {
//                if (entry != null && entry.getKey() != null && entry.getKey().startsWith(Constants.HIDE_KEY_PREFIX)) {
//                    filteredKeys.add(entry.getKey());
//                }
//            }
//            return filteredKeys.toArray(new String[filteredKeys.size()]);
//        } else {
//            return new String[]{};
//        }
//    }
//
//    public ServiceStoreFactory getServiceStoreFactory() {
//        return serviceStoreFactory;
//    }
//
//    public void setServiceStoreFactory(ServiceStoreFactory serviceStoreFactory) {
//        this.serviceStoreFactory = serviceStoreFactory;
//    }
//
//    static private class DestroyableExporter<T> implements Exporter<T> {
//
//        public static final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("Exporter-Unexport", true));
//
//        private ServiceStoreProtocol serviceStoreService;
//        private Invoker<T> originInvoker;
//        private URL subscribeUrl;
//        private URL registerUrl;
//
//        public DestroyableExporter(ServiceStoreProtocol serviceStoreService, Invoker<T> originInvoker, URL subscribeUrl, URL registerUrl) {
//            this.originInvoker = originInvoker;
//            this.subscribeUrl = subscribeUrl;
//            this.registerUrl = registerUrl;
//            this.serviceStoreService = serviceStoreService;
//        }
//
//        @Override
//        public Invoker<T> getInvoker() {
//            return originInvoker;
//        }
//
//        @Override
//        public void unexport() {
//
//            ServiceStore serviceStore = serviceStoreService.getServiceStore(originInvoker);
//            try {
//                serviceStore.remove(registerUrl);
//            } catch (Throwable t) {
//
//            }
//        }
//    }
}
