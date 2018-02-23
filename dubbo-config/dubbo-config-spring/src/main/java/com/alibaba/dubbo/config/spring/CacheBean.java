package com.alibaba.dubbo.config.spring;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;

import com.alibaba.dubbo.config.BufferConfig;
import com.alibaba.dubbo.config.CacheConfig;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;

@SuppressWarnings("rawtypes")
public class CacheBean extends CacheConfig implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener, BeanNameAware {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6107238933448094706L;
	
	@SuppressWarnings("unused")
	private static transient ApplicationContext SPRING_CONTEXT;

	private transient ApplicationContext applicationContext;
	@SuppressWarnings("unused")
    private transient String beanName;
	@SuppressWarnings("unused")
    private transient boolean supportedApplicationListener;

	@Override
	public void setBeanName(String name) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		// TODO Auto-generated method stub
		/*if (ContextRefreshedEvent.class.getName().equals(event.getClass().getName())) {
        	if (! isExported()) {
                
            }
        }*/
	}

	public synchronized void export() {
		doExport();
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		SpringExtensionFactory.addApplicationContext(applicationContext);
		if (applicationContext != null) {
		    SPRING_CONTEXT = applicationContext;
		    try {
	            Method method = applicationContext.getClass().getMethod("addApplicationListener", new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.0.1
	            method.invoke(applicationContext, new Object[] {this});
	            supportedApplicationListener = true;
	        } catch (Throwable t) {
                if (applicationContext instanceof AbstractApplicationContext) {
    	            try {
    	                Method method = AbstractApplicationContext.class.getDeclaredMethod("addListener", new Class<?>[]{ApplicationListener.class}); // 兼容Spring2.0.1
                        if (! method.isAccessible()) {
                            method.setAccessible(true);
                        }
    	                method.invoke(applicationContext, new Object[] {this});
                        supportedApplicationListener = true;
    	            } catch (Throwable t2) {
    	            }
	            }
	        }
		}
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO Auto-generated method stub
		CacheServerBean cacheServer = (CacheServerBean)applicationContext.getBean(server);
	    String protocol = cacheServer.getProtocol();
    	String address = cacheServer.getAddress();
    	String prefix = cacheServer.getPrefix();
    	String user = cacheServer.getUser();
    	String password = cacheServer.getPassword();
    	int maxActive = -1, maxIdle = -1, minIdle = -1, defaultTimeout = -1;
    	if (cacheServer.getMaxactive() != null) {
    		maxActive = Integer.parseInt(cacheServer.getMaxactive());
    	}
    	if (cacheServer.getMaxidle() != null) {
    		maxIdle = Integer.parseInt(cacheServer.getMaxidle());
    	}
    	if (cacheServer.getMinidle() != null) {
    		minIdle = Integer.parseInt(cacheServer.getMinidle());
    	}
    	if (cacheServer.getTimeout() != null) {
    		defaultTimeout = Integer.parseInt(cacheServer.getTimeout());
    	}
    	String connectStr;
    	if (password != null && !password.isEmpty()) {//memcached://user:passwd@127.0.0.1:11211
    		if (user == null) {//redis不需要配置user
    			user = "";
    		}
    		connectStr = protocol + "://" + user + ":" + password + "@" + address;  
    	} else {
    		connectStr = protocol + "://" + address;
        }
    	this.address = connectStr;
    	this.prefix = prefix;
    	this.maxActive = maxActive;
    	this.maxIdle = maxIdle;
    	this.minIdle = minIdle;
    	this.defaultTimeout = defaultTimeout;
    	
    	Class<?> serviceClazz = Class.forName(service);
		Object bean = applicationContext.getBean(serviceClazz);
		String className = bean.getClass().getName();
		if (className.indexOf("$$") != -1) {
			//com.xxx.user.aggregate.impl.UserAggregateImpl$$EnhancerBySpringCGLIB$$efa438ef
			className = className.substring(0, className.indexOf("$$"));
		}
    	for (BufferConfig buffer : bufferList) {
			try {
				String key = buffer.getKey();
				String method = buffer.getMethod();
				String[] methodParamNames = getMethodParamNames(className, method);
				Map<String, Integer> methodParamMap = new HashMap<String, Integer>();
				for (int i=0; i<methodParamNames.length; i++) {
					String methodParamName = methodParamNames[i];
					methodParamMap.put(methodParamName, i+1);
				}
				key = getCacheKey(key, methodParamMap);
				buffer.setKey(key);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private String getCacheKey(String key, Map<String, Integer> methodParamMap) throws Exception {
        String regex = "\\{(.*?)\\}";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(key);
        while (m.find()) {
            String k = m.group();
            if (k.indexOf(".") == -1) {
            	k = k.substring(1, k.length() - 1);
            	if (!methodParamMap.containsKey(k)) {
            		throw new Exception("Cache key : " + key + " - " + k + " does not exist!");
            	}
            	key = key.replaceFirst("\\{" + k + "\\}", "\\{" + methodParamMap.get(k) + "\\}");
            } else {
            	String nk = k.substring(1, k.length() - 1);//1.userid
            	String[] nkAry = nk.split("\\.");
            	if (!methodParamMap.containsKey(nkAry[0])) {
            		throw new Exception("Cache key : " + key + " - " + nkAry[0] + " does not exist!");
            	}
            	key = key.replaceFirst("\\{" + nk + "\\}", "{" + methodParamMap.get(nkAry[0]) + "." + nkAry[1] + "}");
            }
        }
        return key;
    }
	
	/**
     * 
     * <p>
     * 获取方法参数名称
     * </p>
     * 
     * @param cm
     * @return
     */
    private String[] getMethodParamNames(CtMethod cm) throws Exception {
        CtClass cc = cm.getDeclaringClass();
        MethodInfo methodInfo = cm.getMethodInfo();
        CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute
                .getAttribute(LocalVariableAttribute.tag);
        if (attr == null) {
            throw new Exception(cc.getName());
        }
 
        String[] paramNames = null;
        try {
            paramNames = new String[cm.getParameterTypes().length];
        } catch (NotFoundException e) {
            throw e;
        }
        int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
        for (int i = 0; i < paramNames.length; i++) {
            paramNames[i] = attr.variableName(i + pos);
        }
        return paramNames;
    }
 
 
    /**
     * 获取方法参数名称，匹配同名的某一个方法
     * 
     * @param clazz
     * @param method
     * @return
     * @throws NotFoundException
     *             如果类或者方法不存在
     * @throws MissingLVException
     *             如果最终编译的class文件不包含局部变量表信息
     */
    private String[] getMethodParamNames(String className, String method) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        /*ClassClassPath classPath = new ClassClassPath(this.getClass());
        pool.insertClassPath(classPath);*/
        CtClass cc;
        CtMethod cm = null;
        try {
            cc = pool.get(className);
            cm = cc.getDeclaredMethod(method);
        } catch (NotFoundException e) {
        	throw e;
        }
        return getMethodParamNames(cm);
    }

}
