package com.alibaba.dubbo.config.spring.schema.common;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.ArgumentConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.schema.ArgumentBeanDefinitionParser;
import com.alibaba.dubbo.config.spring.schema.MethodBeanDefinitionParser;
import com.alibaba.dubbo.rpc.Protocol;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * DubboBeanDefinitionParser解析器父类
 * @author: zhangyinyue
 * @Createdate: 2017年09月22日 10:38
 */
public abstract class CommonBeanDefinitionParser{

    protected Class<?> beanClass;
    protected boolean required;//id是否一定要有值

    private static final Pattern GROUP_AND_VERION = Pattern.compile("^[\\-.0-9_a-zA-Z]+(\\:[\\-.0-9_a-zA-Z]+)?$");

    //各个属性值字符串
    protected static final String ID = "id";
    protected static final String REGISTRIES = "registries";
    protected static final String PROVIDERS = "providers";
    protected static final String PROTOCOLS = "protocols";
    protected static final String REF = "ref";
    protected static final String ARGUMENTS = "arguments";
    protected static final String ONRETURN = "onreturn";
    protected static final String ONRETURNMETHOD = "onreturnMethod";
    protected static final String ONTHROW = "onthrow";
    protected static final String ONTHROWMETHOD = "onthrowMethod";
    protected static final String PARAMETERS = "parameters";
    protected static final String PROVIDER = "provider";
    protected static final String REGISTRY = "registry";
    protected static final String MONITOR = "monitor";
    protected static final String PROTOCOL = "protocol";
    protected static final String METHODS = "methods";
    protected static final String PARAMETER = "parameter";
    protected static final String METHOD = "method";
    protected static final String NAME = "name";
    protected static final String DEFAULT = "default";
    protected static final String ARGUMENT = "argument";
    protected static final String INDEX = "index";
    protected static final String PROPERTY = "property";
    protected static final String VALUE = "value";
    protected static final String KEY = "key";
    protected static final String HIDE = "hide";
    protected static final String INTERFACE = "interface";
    /**
     * 属性匹配methods
     * @param property
     * @return
     */
    protected boolean matchMethodsRef(String property){
        return METHODS.equals(property);
    }

    /**
     * 属性匹配parameters
     * @param property
     * @return
     */
    protected boolean matchParametersRef(String property){
        return PARAMETERS.equals(property);
    }

    /**
     * 属性匹配provider
     * @param property
     * @param value
     * @return
     */
    protected boolean matchProviderRef(String property, String value){
        return PROVIDER.equals(property) && value.indexOf(',') != -1;
    }

    /**
     * 属性匹配registry,有多个值
     * @param property
     * @param value
     * @return
     */
    protected boolean matchRegistryMultiRef(String property, String value){
        return REGISTRY.equals(property) && value.indexOf(',') != -1;
    }

    /**
     * 属性匹配registry,只有一个值
     * @param property
     * @param value
     * @return
     */
    protected boolean matchRegistry1Ref(String property, String value){
        return REGISTRY.equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value);
    }

    /**
     * 处理匹配registry,只有一个值的情况
     * @param beanDefinition
     * @param property
     */
    protected void processRegistry1Ref(BeanDefinition beanDefinition, String property){
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
        beanDefinition.getPropertyValues().addPropertyValue(property, registryConfig);
    }

    /**
     * 属性匹配monitor
     * @param property
     * @param parserContext
     * @param value
     * @return
     */
    protected boolean matchMonitorRef(String property, ParserContext parserContext, String value){
        return MONITOR.equals(property)
                && (!parserContext.getRegistry().containsBeanDefinition(value)
                || !MonitorConfig.class.getName().equals(parserContext.getRegistry().getBeanDefinition(value).getBeanClassName()));
    }

    /**
     * 属性匹配protocol,有多个值
     * @param property
     * @param value
     * @return
     */
    protected boolean matchProtocolMultiRef(String property, String value){
        return PROTOCOL.equals(property) && value.indexOf(',') != -1;
    }

    /**
     * 处理属性匹配Protocol，并且关联的值只有一个的情况
     * @param element
     * @param logger
     * @param value
     * @return
     */
    protected Object processProtocol1Ref(Element element, Logger logger, String value){
        if ("dubbo:provider".equals(element.getTagName())) {
            logger.warn("Recommended replace <dubbo:provider protocol=\"" + value + "\" ... /> to <dubbo:protocol name=\"" + value + "\" ... />");
        }
        // 兼容旧版本配置
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setName(value);
        return protocol;
    }

    /**
     * 属性匹配Protocol，并且关联的值只有一个
     * @param property
     * @param parserContext
     * @param value
     * @return
     */
    protected boolean matchProtocol1Ref(String property, ParserContext parserContext, String value){
        return PROTOCOL.equals(property)
                && ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(value)
                && (!parserContext.getRegistry().containsBeanDefinition(value)
                || !ProtocolConfig.class.getName().equals(parserContext.getRegistry().getBeanDefinition(value).getBeanClassName()));
    }

    /**
     * 检查getter
     * @param beanClass
     * @param name
     * @param type
     * @return
     */
    protected boolean checkGetter(Class<?> beanClass, String name, Class<?> type){
        Method getter = getGetter(beanClass, name);
       return getter == null || !Modifier.isPublic(getter.getModifiers()) || !type.equals(getter.getReturnType());
    }

    /**
     * 检查setter
     * @param setter
     * @return
     */
    protected boolean checkSetter(Method setter){
        return Modifier.isPublic(setter.getModifiers()) && setter.getParameterTypes().length == 1;
    }

    /**
     * 检查名称合法性
     * @param name
     * @return
     */
    protected boolean checkName(String name){
        return name.length() > 3 && name.startsWith("set");
    }

    /**
     * 获取getter方法
     * @param beanClass
     * @param name
     * @return
     */
    protected Method getGetter(Class<?> beanClass, String name){
        Method getter = null;
        try {
            getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
        } catch (NoSuchMethodException e) {
            try {
                getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
            } catch (NoSuchMethodException e2) {
            }
        }
        return getter;
    }

    /**
     * 供Consumer,Protocol,Provider,Service 这几个config在处理方法属性前，对beanClass的一些处理
     * @param parserContext
     * @param id
     * @param beanDefinition
     * @param element
     */
    protected void dealWithBeanClass(ParserContext parserContext, String id, RootBeanDefinition beanDefinition, Element element){

    }

    /**
     * 处理兼容旧版本xsd中的default值
     * @param property
     * @param value
     * @return
     */
    protected Object processXsdDefault(String property, Object value){
        if ("async".equals(property) && "false".equals(value)
                || "timeout".equals(property) && "0".equals(value)
                || "delay".equals(property) && "0".equals(value)
                || "version".equals(property) && "0.0.0".equals(value)
                || "stat".equals(property) && "-1".equals(value)
                || "reliable".equals(property) && "false".equals(value)) {
            // 兼容旧版本xsd中的default值
            value = null;
        }
        return value;
    }

    /**
     * 具体处理beanDefinition中parameters参数值
     * @param attributes
     * @param props
     * @param parameters
     * @param beanDefinition
     */
    protected void processParameters(NamedNodeMap attributes, Set<String> props, ManagedMap parameters, RootBeanDefinition beanDefinition){
        int len = attributes.getLength();
        for (int i = 0; i < len; i++) {
            Node node = attributes.item(i);
            String name = node.getLocalName();
            if (!props.contains(name)) {
                if (parameters == null) {
                    parameters = new ManagedMap();
                }
                String value = node.getNodeValue();
                parameters.put(name, new TypedStringValue(value, String.class));
            }
        }
        if (parameters.size() != 0) {
            beanDefinition.getPropertyValues().addPropertyValue(PARAMETERS, parameters);
        }
    }

    /**
     * 处理setter方法
     * @param element
     * @param props
     * @param beanClass
     * @param beanDefinition
     * @param parserContext
     * @param parameters
     */
    protected void processSetterMethods(Element element, Set<String> props, Class<?> beanClass, RootBeanDefinition beanDefinition, ParserContext parserContext, ManagedMap parameters){
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName();
            if ( checkName(name) && checkSetter(setter)) {
                Class<?> type = setter.getParameterTypes()[0];
                String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
                props.add(property);
                if (checkGetter(beanClass, name, type)) {
                    continue;
                }
                String value = element.getAttribute(property);
                processValue(value, property, beanDefinition, parserContext, type, element, parameters);
            }
        }
    }

    /**
     * 对id值放到beanDefinition的处理
     * @param id
     * @param parserContext
     * @param beanDefinition
     */
    protected void processBeanDefinition(String id, ParserContext parserContext, RootBeanDefinition beanDefinition){
        if (StringUtils.isNotEmpty(id)) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
            beanDefinition.getPropertyValues().addPropertyValue(ID, id);
        }
    }

    /**
     * 对id值的处理
     * @param element
     * @param parserContext
     * @param beanClass
     * @param id
     * @return
     */
    protected String processId(Element element, ParserContext parserContext, Class<?> beanClass, String id){
        if (StringUtils.isBlank(id)) {
            String generatedBeanName = element.getAttribute(NAME);
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                if (ProtocolConfig.class.equals(beanClass)) {
                    generatedBeanName = "dubbo";
                } else {
                    generatedBeanName = element.getAttribute(INTERFACE);
                }
            }
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            int counter = 2;
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        return id;
    }

    /**
     * 各个解析器处理具体值,需各个子类实现
     * @param value 属性值
     * @param property 属性名称
     * @param beanDefinition beanDefinition数据
     * @param parserContext 解析上下文
     * @param type setter方法参数类型
     * @param element xml 节点
     * @param parameters 自定义参数
     */
    protected abstract void processValue(String value, String property, RootBeanDefinition beanDefinition, ParserContext parserContext, Class<?> type, Element element, ManagedMap parameters);


    /**
     * 实现BeanDefinitionParser接口方法
     * @param element
     * @param parserContext
     * @return
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return parse(element, parserContext, beanClass);
    }

    /**
     * 解析主流程
     * @param element
     * @param parserContext
     * @param beanClass
     * @return
     */
    protected BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        String id = element.getAttribute(ID);
        if (required) {
            id = processId(element,parserContext,beanClass,id);
        }
        processBeanDefinition(id, parserContext, beanDefinition);

        dealWithBeanClass(parserContext, id, beanDefinition, element);
        Set<String> props = new HashSet<String>();
        ManagedMap parameters = new ManagedMap();
        processSetterMethods(element, props, beanClass, beanDefinition, parserContext, parameters);

        NamedNodeMap attributes = element.getAttributes();
        processParameters(attributes, props, parameters, beanDefinition);
        return beanDefinition;
    }

    /**
     * 解析benaDefinition的参数
     * @param nodeList
     * @param beanDefinition
     * @param parameters
     */
    @SuppressWarnings("unchecked")
    protected static void parseParameters(NodeList nodeList, RootBeanDefinition beanDefinition, ManagedMap parameters) {
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if (PARAMETER.equals(node.getNodeName())
                            || PARAMETER.equals(node.getLocalName())) {
                        String key = ((Element) node).getAttribute(KEY);
                        String value = ((Element) node).getAttribute(VALUE);
                        boolean hide = "true".equals(((Element) node).getAttribute(HIDE));
                        if (hide) {
                            key = Constants.HIDE_KEY_PREFIX + key;
                        }
                        parameters.put(key, new TypedStringValue(value, String.class));
                    }
                }
            }
        }
    }

    /**
     * 解析方法标签
     * @param id
     * @param nodeList
     * @param beanDefinition
     * @param parserContext
     */
    @SuppressWarnings("unchecked")
    protected static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                     ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList methods = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if (METHOD.equals(node.getNodeName()) || METHOD.equals(node.getLocalName())) {
                        String methodName = element.getAttribute(NAME);
                        if (methodName == null || methodName.length() == 0) {
                            throw new IllegalStateException("<dubbo:method> name attribute == null");
                        }
                        if (methods == null) {
                            methods = new ManagedList();
                        }
                        BeanDefinition methodBeanDefinition = new MethodBeanDefinitionParser(MethodConfig.class,false).parse(((Element) node),parserContext) ;//parse(((Element) node),parserContext, MethodConfig.class, false);
                        String name = id + "." + methodName;
                        BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(
                                methodBeanDefinition, name);
                        methods.add(methodBeanDefinitionHolder);
                    }
                }
            }
            if (methods != null) {
                beanDefinition.getPropertyValues().addPropertyValue(METHODS, methods);
            }
        }
    }

    /**
     * 解析嵌套情况
     * @param element
     * @param parserContext
     * @param beanClass
     * @param required
     * @param tag
     * @param property
     * @param ref
     * @param beanDefinition
     */
    protected void parseNested(Element element, ParserContext parserContext, Class<?> beanClass, boolean required, String tag, String property, String ref, BeanDefinition beanDefinition) {
        NodeList nodeList = element.getChildNodes();
        if (nodeList != null && nodeList.getLength() > 0) {
            boolean first = true;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if (tag.equals(node.getNodeName())
                            || tag.equals(node.getLocalName())) {
                        if (first) {
                            first = false;
                            String isDefault = element.getAttribute(DEFAULT);
                            if (isDefault == null || isDefault.length() == 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(DEFAULT, "false");
                            }
                        }
                        BeanDefinition subDefinition = parse((Element) node, parserContext, beanClass);
                        if (subDefinition != null && ref != null && ref.length() > 0) {
                            subDefinition.getPropertyValues().addPropertyValue(property, new RuntimeBeanReference(ref));
                        }
                    }
                }
            }
        }
    }

    /**
     * 解析方法参数
     * @param id
     * @param nodeList
     * @param beanDefinition
     * @param parserContext
     */
    @SuppressWarnings("unchecked")
    protected static void parseArguments(String id, NodeList nodeList, RootBeanDefinition beanDefinition,
                                       ParserContext parserContext) {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList arguments = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if (ARGUMENT.equals(node.getNodeName()) || ARGUMENT.equals(node.getLocalName())) {
                        String argumentIndex = element.getAttribute(INDEX);
                        if (arguments == null) {
                            arguments = new ManagedList();
                        }
                        BeanDefinition argumentBeanDefinition = new ArgumentBeanDefinitionParser(ArgumentConfig.class,false).parse(((Element) node),parserContext);//parse(((Element) node),parserContext, ArgumentConfig.class, false);
                        String name = id + "." + argumentIndex;
                        BeanDefinitionHolder argumentBeanDefinitionHolder = new BeanDefinitionHolder(
                                argumentBeanDefinition, name);
                        arguments.add(argumentBeanDefinitionHolder);
                    }
                }
            }
            if (arguments != null) {
                beanDefinition.getPropertyValues().addPropertyValue(ARGUMENTS, arguments);
            }
        }
    }

    /**
     * 解析多关联
     * @param property
     * @param value
     * @param beanDefinition
     * @param parserContext
     */
    @SuppressWarnings("unchecked")
    protected static void parseMultiRef(String property, String value, RootBeanDefinition beanDefinition,
                                      ParserContext parserContext) {
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (int i = 0; i < values.length; i++) {
            String v = values[i];
            if (v != null && v.length() > 0) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    /**
     * 是否为原型
     * @param cls
     * @return
     */
    protected static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == Boolean.class || cls == Byte.class
                || cls == Character.class || cls == Short.class || cls == Integer.class
                || cls == Long.class || cls == Float.class || cls == Double.class
                || cls == String.class || cls == Date.class || cls == Class.class;
    }

    /**
     * 转换为监控Monitor
     * @param monitor
     * @return
     */
    protected static MonitorConfig convertMonitor(String monitor) {
        if (monitor == null || monitor.length() == 0) {
            return null;
        }
        if (GROUP_AND_VERION.matcher(monitor).matches()) {
            String group;
            String version;
            int i = monitor.indexOf(':');
            if (i > 0) {
                group = monitor.substring(0, i);
                version = monitor.substring(i + 1);
            } else {
                group = monitor;
                version = null;
            }
            MonitorConfig monitorConfig = new MonitorConfig();
            monitorConfig.setGroup(group);
            monitorConfig.setVersion(version);
            return monitorConfig;
        }
        return null;
    }

    /**
     * 解析属性值
     * @param nodeList
     * @param beanDefinition
     */
    protected static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if (PROPERTY.equals(node.getNodeName())
                            || PROPERTY.equals(node.getLocalName())) {
                        String name = ((Element) node).getAttribute(NAME);
                        if (name != null && name.length() > 0) {
                            String value = ((Element) node).getAttribute(VALUE);
                            String ref = ((Element) node).getAttribute(REF);
                            if (value != null && value.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, value);
                            } else if (ref != null && ref.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                            } else {
                                throw new UnsupportedOperationException("Unsupported <property name=\"" + name + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\"" + name + "\" value=\"...\" />");
                            }
                        }
                    }
                }
            }
        }
    }


}