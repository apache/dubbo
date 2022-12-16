package org.apache.dubbo.rpc.protocol.rest.request;


public class RequestFacadeFactory {
    private final static String JakartaServlet = "jakarta.servlet.http.HttpServletRequest";
    private final static String JavaxServlet = "javax.servlet.http.HttpServletRequest";


    public static RequestFacade createRequestFacade(Object request) {

        if (tryLoad(JavaxServlet, request)) {

            return new JavaxServletRequestFacade(request);
        }

        if (tryLoad(JakartaServlet, request)) {
            return new JakartaServletRequestFacade(request);
        }


        throw new RuntimeException("no compatible  ServletRequestFacade and request type is " + request.getClass());

    }

    public static boolean tryLoad(String requestClassName, Object request) {

        ClassLoader classLoader = request.getClass().getClassLoader();

        try {
            Class<?> requestClass = classLoader.loadClass(requestClassName);

            return requestClass.isAssignableFrom(requestClass);

        } catch (ClassNotFoundException e) {
            return false;
        }

    }


}
