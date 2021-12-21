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
package org.apache.dubbo.config;

import org.apache.dubbo.common.config.Environment;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Method;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.AsyncMethodInfo;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.dubbo.config.Constants.ON_INVOKE_INSTANCE_PARAMETER_KEY;
import static org.apache.dubbo.config.Constants.ON_INVOKE_METHOD_PARAMETER_KEY;
import static org.apache.dubbo.config.Constants.ON_RETURN_INSTANCE_PARAMETER_KEY;
import static org.apache.dubbo.config.Constants.ON_RETURN_METHOD_PARAMETER_KEY;
import static org.apache.dubbo.config.Constants.ON_THROW_INSTANCE_PARAMETER_KEY;
import static org.apache.dubbo.config.Constants.ON_THROW_METHOD_PARAMETER_KEY;

/**
 * The method configuration
 *
 * @export
 */
public class MethodConfig extends AbstractMethodConfig {

    private static final long serialVersionUID = 884908855422675941L;

    /**
     * The method name
     */
    private String name;

    /**
     * Stat
     */
    private Integer stat;

    /**
     * Whether to retry
     */
    private Boolean retry;

    /**
     * If it's reliable
     */
    private Boolean reliable;

    /**
     * Thread limits for method invocations
     */
    private Integer executes;

    /**
     * If it's deprecated
     */
    private Boolean deprecated;

    /**
     * Whether to enable sticky
     */
    private Boolean sticky;

    /**
     * Whether you need to return
     */
    private Boolean isReturn;

    /**
     * Callback instance when async-call is invoked
     */
    private Object oninvoke;

    /**
     * Callback method when async-call is invoked
     */
    private String oninvokeMethod;

    /**
     * Callback instance when async-call is returned
     */
    private Object onreturn;

    /**
     * Callback method when async-call is returned
     */
    private String onreturnMethod;

    /**
     * Callback instance when async-call has exception thrown
     */
    private Object onthrow;

    /**
     * Callback method when async-call has exception thrown
     */
    private String onthrowMethod;

    /**
     * The method arguments
     */
    private List<ArgumentConfig> arguments;

    /**
     * TODO remove service and serviceId
     * These properties come from MethodConfig's parent Config module, they will neither be collected directly from xml or API nor be delivered to url
     */
    private String service;
    private String serviceId;

    /**
     * The preferred prefix of parent
     */
    private String parentPrefix;


    public MethodConfig() {
    }

    public MethodConfig(ModuleModel moduleModel) {
        super(moduleModel);
    }

    /**
     * TODO remove this construct, the callback method processing logic needs to rely on Spring context
     */
    @Deprecated
    public MethodConfig(Method method) {
        appendAnnotation(Method.class, method);

        this.setReturn(method.isReturn());

        String split = ".";

        if (!"".equals(method.oninvoke()) && method.oninvoke().lastIndexOf(split) > 0) {
            int index = method.oninvoke().lastIndexOf(split);
            String ref = method.oninvoke().substring(0, index);
            String methodName = method.oninvoke().substring(index + 1);
            this.setOninvoke(ref);
            this.setOninvokeMethod(methodName);
        }
        if (!"".equals(method.onreturn()) && method.onreturn().lastIndexOf(split) > 0) {
            int index = method.onreturn().lastIndexOf(split);
            String ref = method.onreturn().substring(0, index);
            String methodName = method.onreturn().substring(index + 1);
            this.setOnreturn(ref);
            this.setOnreturnMethod(methodName);
        }
        if (!"".equals(method.onthrow()) && method.onthrow().lastIndexOf(split) > 0) {
            int index = method.onthrow().lastIndexOf(split);
            String ref = method.onthrow().substring(0, index);
            String methodName = method.onthrow().substring(index + 1);
            this.setOnthrow(ref);
            this.setOnthrowMethod(methodName);
        }

        if (method.arguments() != null && method.arguments().length != 0) {
            List<ArgumentConfig> argumentConfigs = new ArrayList<ArgumentConfig>(method.arguments().length);
            this.setArguments(argumentConfigs);
            for (int i = 0; i < method.arguments().length; i++) {
                ArgumentConfig argumentConfig = new ArgumentConfig(method.arguments()[i]);
                argumentConfigs.add(argumentConfig);
            }
        }
    }

    /**
     * TODO remove constructMethodConfig
     *
     * @param methods
     * @return
     */
    @Deprecated
    public static List<MethodConfig> constructMethodConfig(Method[] methods) {
        if (methods != null && methods.length != 0) {
            List<MethodConfig> methodConfigs = new ArrayList<>(methods.length);
            for (int i = 0; i < methods.length; i++) {
                MethodConfig methodConfig = new MethodConfig(methods[i]);
                methodConfigs.add(methodConfig);
            }
            return methodConfigs;
        }
        return Collections.emptyList();
    }

    /**
     * Get method prefixes
     *
     * @return
     */
    @Override
    @Parameter(excluded = true, attribute = false)
    public List<String> getPrefixes() {
        // parent prefix + method name
        if (parentPrefix != null) {
            List<String> prefixes = new ArrayList<>();
            prefixes.add(parentPrefix + "." + this.getName());
            return prefixes;
        } else {
            throw new IllegalStateException("The parent prefix of MethodConfig is null");
        }
    }

    @Override
    protected void processExtraRefresh(String preferredPrefix, InmemoryConfiguration subPropsConfiguration) {
        // refresh ArgumentConfigs
        if (this.getArguments() != null && this.getArguments().size() > 0) {
            for (ArgumentConfig argument : this.getArguments()) {
                refreshArgument(argument, subPropsConfiguration);
            }
        }
    }

    private void refreshArgument(ArgumentConfig argument, InmemoryConfiguration subPropsConfiguration) {
        if (argument.getIndex() != null && argument.getIndex() >= 0) {
            String prefix = argument.getIndex() + ".";
            Environment environment = getScopeModel().getModelEnvironment();
            List<java.lang.reflect.Method> methods = MethodUtils.getMethods(argument.getClass(),
                method -> method.getDeclaringClass() != Object.class);
            for (java.lang.reflect.Method method : methods) {
                if (MethodUtils.isSetter(method)) {
                    String propertyName = extractPropertyName(method.getName());
                    // ignore attributes: 'index' / 'type'
                    if (StringUtils.isEquals(propertyName, "index") ||
                        StringUtils.isEquals(propertyName, "type")) {
                        continue;
                    }
                    // convert camelCase/snake_case to kebab-case
                    String kebabPropertyName = prefix + StringUtils.convertToSplitName(propertyName, "-");

                    try {
                        String value = StringUtils.trim(subPropsConfiguration.getString(kebabPropertyName));
                        if (StringUtils.hasText(value) && ClassUtils.isTypeMatch(method.getParameterTypes()[0], value)) {
                            value = environment.resolvePlaceholders(value);
                            method.invoke(argument, ClassUtils.convertPrimitive(method.getParameterTypes()[0], value));
                        }
                    } catch (Exception e) {
                        logger.info("Failed to override the property " + method.getName() + " in " +
                            this.getClass().getSimpleName() +
                            ", please make sure every property has getter/setter method provided.");
                    }
                }
            }
        }
    }


    public AsyncMethodInfo convertMethodConfig2AsyncInfo() {
        if ((getOninvoke() == null && getOnreturn() == null && getOnthrow() == null)) {
            return null;
        }

        //check config conflict
        if (Boolean.FALSE.equals(isReturn()) && (getOnreturn() != null || getOnthrow() != null)) {
            throw new IllegalStateException("method config error : return attribute must be set true when on-return or on-throw has been set.");
        }

        AsyncMethodInfo asyncMethodInfo = new AsyncMethodInfo();

        asyncMethodInfo.setOninvokeInstance(getOninvoke());
        asyncMethodInfo.setOnreturnInstance(getOnreturn());
        asyncMethodInfo.setOnthrowInstance(getOnthrow());

        try {
            if (StringUtils.isNotEmpty(oninvokeMethod)) {
                asyncMethodInfo.setOninvokeMethod(getMethodByName(getOninvoke().getClass(), oninvokeMethod));
            }

            if (StringUtils.isNotEmpty(onreturnMethod)) {
                asyncMethodInfo.setOnreturnMethod(getMethodByName(getOnreturn().getClass(), onreturnMethod));
            }

            if (StringUtils.isNotEmpty(onthrowMethod)) {
                asyncMethodInfo.setOnthrowMethod(getMethodByName(getOnthrow().getClass(), onthrowMethod));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        return asyncMethodInfo;
    }

    private java.lang.reflect.Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set default field values of MethodConfig.
     *
     * @see org.apache.dubbo.config.annotation.Method
     */
    @Override
    protected void checkDefault() {
        super.checkDefault();

        // set default field values
        // org.apache.dubbo.config.annotation.Method.isReturn() default true;
        if (isReturn() == null) {
            setReturn(true);
        }

        // org.apache.dubbo.config.annotation.Method.sent() default true;
        if (getSent() == null) {
            setSent(true);
        }
    }

    @Parameter(excluded = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        // FIXME, add id strategy in ConfigManager
//        if (StringUtils.isEmpty(id)) {
//            id = name;
//        }
    }

    public Integer getStat() {
        return stat;
    }

    @Deprecated
    public void setStat(Integer stat) {
        this.stat = stat;
    }

    @Deprecated
    public Boolean isRetry() {
        return retry;
    }

    @Deprecated
    public void setRetry(Boolean retry) {
        this.retry = retry;
    }

    @Deprecated
    public Boolean isReliable() {
        return reliable;
    }

    @Deprecated
    public void setReliable(Boolean reliable) {
        this.reliable = reliable;
    }

    public Integer getExecutes() {
        return executes;
    }

    public void setExecutes(Integer executes) {
        this.executes = executes;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<ArgumentConfig> getArguments() {
        return arguments;
    }

    @SuppressWarnings("unchecked")
    public void setArguments(List<? extends ArgumentConfig> arguments) {
        this.arguments = (List<ArgumentConfig>) arguments;
    }

    public Boolean getSticky() {
        return sticky;
    }

    public void setSticky(Boolean sticky) {
        this.sticky = sticky;
    }

    @Parameter(key = ON_RETURN_INSTANCE_PARAMETER_KEY, excluded = true, attribute = true)
    public Object getOnreturn() {
        return onreturn;
    }

    public void setOnreturn(Object onreturn) {
        this.onreturn = onreturn;
    }

    @Parameter(key = ON_RETURN_METHOD_PARAMETER_KEY, excluded = true, attribute = true)
    public String getOnreturnMethod() {
        return onreturnMethod;
    }

    public void setOnreturnMethod(String onreturnMethod) {
        this.onreturnMethod = onreturnMethod;
    }

    @Parameter(key = ON_THROW_INSTANCE_PARAMETER_KEY, excluded = true, attribute = true)
    public Object getOnthrow() {
        return onthrow;
    }

    public void setOnthrow(Object onthrow) {
        this.onthrow = onthrow;
    }

    @Parameter(key = ON_THROW_METHOD_PARAMETER_KEY, excluded = true, attribute = true)
    public String getOnthrowMethod() {
        return onthrowMethod;
    }

    public void setOnthrowMethod(String onthrowMethod) {
        this.onthrowMethod = onthrowMethod;
    }

    @Parameter(key = ON_INVOKE_INSTANCE_PARAMETER_KEY, excluded = true, attribute = true)
    public Object getOninvoke() {
        return oninvoke;
    }

    public void setOninvoke(Object oninvoke) {
        this.oninvoke = oninvoke;
    }

    @Parameter(key = ON_INVOKE_METHOD_PARAMETER_KEY, excluded = true, attribute = true)
    public String getOninvokeMethod() {
        return oninvokeMethod;
    }

    public void setOninvokeMethod(String oninvokeMethod) {
        this.oninvokeMethod = oninvokeMethod;
    }

    public Boolean isReturn() {
        return isReturn;
    }

    public void setReturn(Boolean isReturn) {
        this.isReturn = isReturn;
    }

    @Parameter(excluded = true, attribute = false)
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Parameter(excluded = true, attribute = false)
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setParentPrefix(String parentPrefix) {
        this.parentPrefix = parentPrefix;
    }

    @Parameter(excluded = true, attribute = false)
    public String getParentPrefix() {
        return parentPrefix;
    }

    public void addArgument(ArgumentConfig argumentConfig) {
        if (arguments == null) {
            arguments = new ArrayList<>();
        }
        arguments.add(argumentConfig);
    }
}
