package org.apache.dubbo.rpc.spi;

/**
 *
 */
//public class CheckMock implements SpiMethod {
//
//    @Override
//    public SpiMethodNames methodName() {
//        return null;
//    }
//
//    @Override
//    public boolean attachToApplication() {
//        return false;
//    }
//
//    @Override
//    public Object invoke(Object... params) {
//
//        String mock = (String) params[0];
//        AbstractInterfaceConfig config = (AbstractInterfaceConfig) params[1];
//        Class<?> interfaceClass = (Class<?>) params[2];
//
//        String normalizedMock = MockInvoker.normalizeMock(mock);
//        if (normalizedMock.startsWith(RETURN_PREFIX)) {
//            normalizedMock = normalizedMock.substring(RETURN_PREFIX.length()).trim();
//            try {
//                //Check whether the mock value is legal, if it is illegal, throw exception
//                MockInvoker.parseMockValue(normalizedMock);
//            } catch (Exception e) {
//                throw new IllegalStateException("Illegal mock return in <dubbo:service/reference ... " +
//                    "mock=\"" + mock + "\" />");
//            }
//        } else if (normalizedMock.startsWith(THROW_PREFIX)) {
//            normalizedMock = normalizedMock.substring(THROW_PREFIX.length()).trim();
//            if (ConfigUtils.isNotEmpty(normalizedMock)) {
//                try {
//                    //Check whether the mock value is legal
//                    MockInvoker.getThrowable(normalizedMock);
//                } catch (Exception e) {
//                    throw new IllegalStateException("Illegal mock throw in <dubbo:service/reference ... " +
//                        "mock=\"" + mock + "\" />");
//                }
//            }
//        } else {
//            //Check whether the mock class is an implementation of the interfaceClass, and if it has a default constructor
//            MockInvoker.getMockObject(config.getScopeModel().getExtensionDirector(), normalizedMock, interfaceClass);
//        }
//        return null;
//    }
//}
