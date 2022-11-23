package org.apache.dubbo.rpc.protocol.mvc.servlet;


import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MvcDispatcherServlet extends DispatcherServlet {


    private ServletConfig config;

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.config = config;
    }


    public ServletConfig getServletConfig() {
        return this.config;
    }

    private MvcRequestMappingHandlerMapping requestMappingHandlerMapping;
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;


    public MvcDispatcherServlet() {
        super();
        this.requestMappingHandlerMapping = new MvcRequestMappingHandlerMapping();
        this.requestMappingHandlerAdapter = new MvcRequestMappingHandlerAdapter();
        setPublishEvents(false);
    }


    protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        return requestMappingHandlerAdapter;
    }

    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        return requestMappingHandlerMapping.getHandler(request);
    }

    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        if (logger.isDebugEnabled()) {
//            String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
//            logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
//                    " processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
//        }

        // Keep a snapshot of the request attributes in case of an include,
        // to be able to restore the original attributes after the include.
//        Map<String, Object> attributesSnapshot = null;
//        if (WebUtils.isIncludeRequest(request)) {
//            attributesSnapshot = new HashMap<String, Object>();
//            Enumeration<?> attrNames = request.getAttributeNames();
//            while (attrNames.hasMoreElements()) {
//                String attrName = (String) attrNames.nextElement();
//                if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
//                    attributesSnapshot.put(attrName, request.getAttribute(attrName));
//                }
//            }
//        }

//        // Make framework objects available to handlers and view objects.
//        request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
//        request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
//        request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
//        request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

//        FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
//        if (inputFlashMap != null) {
//            request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
//        }
//        request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
//        request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

//        try {
        doDispatch(request, response);
//        }
//        finally {
//            if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
//                // Restore the original attribute snapshot, in case of an include.
//                if (attributesSnapshot != null) {
//                    restoreAttributesAfterInclude(request, attributesSnapshot);
//                }
//            }
//    }
    }


    public void handlerParse(Object handler) {
        this.requestMappingHandlerMapping.parseHandler(handler);
    }


}
