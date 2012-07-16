/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.common.module.screen;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.utils.CompatibleTypeUtils;
import com.alibaba.dubbo.governance.biz.common.i18n.MessageResourceService;
import com.alibaba.dubbo.governance.web.common.pulltool.RootContextPath;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;

/**
 * BaseScreen
 * 
 * @author william.liangf
 */
public abstract class Restful {
	
	protected static final Logger logger = Logger.getLogger(Restful.class);

	protected static final Pattern SPACE_SPLIT_PATTERN = Pattern.compile("\\s+");

	@Autowired
	private MessageResourceService messageResourceService;
	
	public String getMessage(String key, Object... args) {
		return messageResourceService.getMessage(key, args);
	}
	
	//FIXME 把这些辅助方法提取出去
	protected String role = null;
	protected String operator = null;
	protected User currentUser = null;
	protected String operatorAddress = null;
	protected String currentRegistry = null;
    
    public void execute(Map<String, Object> context) throws Throwable {
        if(context.get(WebConstants.CURRENT_USER_KEY)!=null){
        	User user = (User) context.get(WebConstants.CURRENT_USER_KEY);
        	currentUser = user;
        	operator = user.getUsername();
        	role = user.getRole();
        	context.put(WebConstants.CURRENT_USER_KEY, user);
        }
        operatorAddress = (String)context.get("request.remoteHost");
        context.put("operator", operator);
    	context.put("operatorAddress", operatorAddress);
    	
        context.put("currentRegistry", currentRegistry);
        
        String httpMethod = (String) context.get("request.method");
        String method = (String) context.get("_method");
        String contextPath = (String) context.get("request.contextPath");
        context.put("rootContextPath", new RootContextPath(contextPath));
        
        // 分析Method
        if (method == null || method.length() == 0) {
            String id = (String) context.get("id");
            if(id == null || id.length() == 0) {
                method = "index";
            }
            else {
                method = "show";
            }
        }
        if ("index".equals(method)) {
            if("post".equalsIgnoreCase(httpMethod)) {
                method = "create";
            }
        } else if ("show".equals(method)) {
            if("put".equalsIgnoreCase(httpMethod) || "post".equalsIgnoreCase(httpMethod)) { // 因表单不能提交PUT请求，用POST代替
                method = "update";
            } else if ("delete".equalsIgnoreCase(httpMethod)) { // 因表单不能提交DELETE请求，用参数代替
                method = "delete";
            }
        }
        context.put("_method", method);
        
        try {
            Method m = null;
            try {
                m = getClass().getMethod(method, new Class<?>[]{Map.class});
            } catch (NoSuchMethodException e) {
                for (Method mtd : getClass().getMethods()) {
                    if (Modifier.isPublic(mtd.getModifiers()) 
                            && mtd.getName().equals(method)) {
                        m = mtd;
                        break;
                    }
                }
                if (m == null) {
                    throw e;
                }
            }
            if (m.getParameterTypes().length > 2) {
                throw new IllegalStateException("Unsupport restful method " + m);
            } else if (m.getParameterTypes().length == 2
                    && (m.getParameterTypes()[0].equals(Map.class) 
                            || ! m.getParameterTypes()[1].equals(Map.class))) {
                throw new IllegalStateException("Unsupport restful method " + m);
            }
            Object r;
            if (m.getParameterTypes().length == 0) {
                r = m.invoke(this, new Object[0]);
            } else {
                Object value;
                Class<?> t = m.getParameterTypes()[0];
                if (Map.class.equals(t)) {
                    value = context;
                } else if (isPrimitive(t)) {
                    String id = (String) context.get("id");
                    value = convertPrimitive(t, id);
                } else if (t.isArray() && isPrimitive(t.getComponentType())) {
                    String id = (String) context.get("id");
                    String[] ids = id == null ? new String[0] : id.split("[.+]+");
                    value = Array.newInstance(t.getComponentType(), ids.length);
                    for (int i = 0; i < ids.length; i ++) {
                        Array.set(value, i, convertPrimitive(t.getComponentType(), ids[i]));
                    }
                } else {
                    value = t.newInstance();
                    for (Method mtd : t.getMethods()) {
                        if (Modifier.isPublic(mtd.getModifiers()) 
                                && mtd.getName().startsWith("set")
                                && mtd.getParameterTypes().length == 1) {
                            String p = mtd.getName().substring(3, 4).toLowerCase() + mtd.getName().substring(4);
                            Object v = context.get(p);
                            if (v == null) {
                                if ("operator".equals(p)) {
                                    v = operator;
                                } else if ("operatorAddress".equals(p)) {
                                    v = (String) context.get("request.remoteHost");
                                }
                            }
                            if (v != null) {
                            	try {
                            		mtd.invoke(value, new Object[] { CompatibleTypeUtils.compatibleTypeConvert(v, mtd.getParameterTypes()[0]) });
                            	} catch (Throwable e) {
                            		logger.warn(e.getMessage(), e);
                            	}
                            }
                        }
                    }
                }
                if (m.getParameterTypes().length == 1) {
                    r = m.invoke(this, new Object[] {value});
                } else {
                    r = m.invoke(this, new Object[] {value, context});
                }
            }
            if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
                context.put("rundata.layout", "redirect");
                context.put("rundata.target", "redirect");
                context.put("success", r == null || ((Boolean) r).booleanValue());
                if(context.get("redirect")==null){
                	context.put("redirect", getDefaultRedirect(context, method));
                }
            } else if (m.getReturnType() == String.class) {
                String redirect = (String) r;
                if (redirect == null) {
                    redirect = getDefaultRedirect(context, method);
                }
                
                if(context.get("chain")!=null){
                	context.put("rundata.layout", "home");
                    context.put("rundata.target", "home");
                }else{
                	context.put("rundata.redirect", redirect);
                }
            } else {
                context.put("rundata.layout", method);
                context.put("rundata.target", context.get("rundata.target") + "/" + method);
            }
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                throw ((InvocationTargetException) e).getTargetException();
            }
//            if (e instanceof InvocationTargetException) {
//                e = ((InvocationTargetException) e).getTargetException();
//            }
//            logger.warn(e.getMessage(), e);
//            context.put("rundata.layout", "redirect");
//            context.put("rundata.target", "redirect");
//            context.put("success", false);
//            context.put("exception", e);
//            context.put("redirect", getDefaultRedirect(context, method));
        }
    }
    
	private static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive()|| cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class;
    }
    
    private static Object convertPrimitive(Class<?> cls, String value) {
        if (cls == boolean.class || cls == Boolean.class) {
            return value == null || value.length() == 0 ? false : Boolean.valueOf(value);
        } else if (cls == byte.class || cls == Byte.class) {
            return value == null || value.length() == 0 ? 0 : Byte.valueOf(value);
        } else if (cls == char.class || cls == Character.class) {
            return value == null || value.length() == 0 ? '\0' : value.charAt(0);
        } else if (cls == short.class || cls == Short.class) {
            return value == null || value.length() == 0 ? 0 : Short.valueOf(value);
        } else if (cls == int.class || cls == Integer.class) {
            return value == null || value.length() == 0 ? 0 : Integer.valueOf(value);
        } else if (cls == long.class || cls == Long.class) {
            return value == null || value.length() == 0 ? 0 : Long.valueOf(value);
        } else if (cls == float.class || cls == Float.class) {
            return value == null || value.length() == 0 ? 0 : Float.valueOf(value);
        } else if (cls == double.class || cls == Double.class) {
            return value == null || value.length() == 0 ? 0 : Double.valueOf(value);
        }
        return value;
    }
    
    private String getDefaultRedirect(Map<String, Object> context, String operate) {
        String defaultRedirect = (String) context.get("defaultRedirect");
            return defaultRedirect;
    }
   
}
