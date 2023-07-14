package org.apache.dubbo.rpc.spi;

//public class DestroyProtocols implements SpiMethod {
//
//    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DestroyProtocols.class);
//
//    @Override
//    public SpiMethodNames methodName() {
//        return SpiMethodNames.destroyProtocols;
//    }
//
//    @Override
//    public boolean attachToApplication() {
//        return false;
//    }
//
//    @Override
//    public Object invoke(Object... params) {
//        destroyProtocols((FrameworkModel) params[0],(AtomicBoolean) params[1]);
//        return null;
//    }
//
//    private void destroyProtocols(FrameworkModel frameworkModel, AtomicBoolean protocolDestroyed) {
//        if (protocolDestroyed.compareAndSet(false, true)) {
//            ExtensionLoader<Protocol> loader = frameworkModel.getExtensionLoader(Protocol.class);
//            for (String protocolName : loader.getLoadedExtensions()) {
//                try {
//                    Protocol protocol = loader.getLoadedExtension(protocolName);
//                    if (protocol != null) {
//                        protocol.destroy();
//                    }
//                } catch (Throwable t) {
//                    logger.warn(CONFIG_UNDEFINED_PROTOCOL, "", "", t.getMessage(), t);
//                }
//            }
//        }
//    }
//}
