/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufServices.java
 * 创建人:xichen
 * 创建日期:2015年12月21日-下午10:32:07
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.utils.ClassHelper;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.google.protobuf.Message;

/**
 * 类名称:ProtobufServices 类描述:暂时只处理同步调用 创建人:xichen 修改人:xichen 修改时间:2015年12月21日-下午10:32:07 修改备注:
 */
public class ProtobufServices {

  // 阻塞调用信息
  private final static String BLOCKING_SERVICE = "$BlockingInterface";
  private final static String PROTOBUF_CONTROLLER = "com.google.protobuf.RpcController";

  // 获取默认类型对象，越过build模式
  private static final String DEFAULT_INSTANCE_METHOD = "getDefaultInstance";

  // service中method输入输出class类型
  private final ConcurrentMap<String, Class<?>> serviceRequestClassMap = new ConcurrentHashMap<String, Class<?>>();
  private final ConcurrentMap<String, Class<?>> serviceResponseClassMap = new ConcurrentHashMap<String, Class<?>>();

  private final ConcurrentHashMap<String, com.google.protobuf.Message> serviceRequestTypeMap = new ConcurrentHashMap<String, Message>();
  private final ConcurrentHashMap<String, com.google.protobuf.Message> serviceResponseTypeMap = new ConcurrentHashMap<String, Message>();

  private static ProtobufServices instance = new ProtobufServices();

  public static ProtobufServices getInstance() {
    return instance;
  }

  private ProtobufServices() {}

  // service接口方法接口
  private String getServiceKey(String service, String method) {
    return service + ":" + method;
  }

  // 暴露出service注册接口
  public <T> void addService(Invoker<T> invoker) {
    addService(invoker.getInterface().getName());
  }

  // 为provider注册服务
  public void addService(String serviceName) {

    // 找出接口信息
    if (!serviceName.endsWith(BLOCKING_SERVICE)) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "service interface errors,name:" + serviceName);
    }

    // 找出class信息
    Class<?> serviceClazz;
    try {
      serviceClazz = ClassHelper.forNameWithThreadContextClassLoader(serviceName);
    } catch (ClassNotFoundException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    addService(serviceClazz);
  }

  // 为consumer注册服务
  public <T> void addService(Class<T> serviceClazz) {
    // 找出所有接口方法
    Method[] serviceMethods;
    try {
      serviceMethods = serviceClazz.getMethods();
    } catch (SecurityException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    // 遍历所有方法
    for (Method method : serviceMethods) {
      Class<?>[] paraClazz = method.getParameterTypes();
      // 检查方法参数长度
      if (paraClazz.length != ProtobufConstants.METHOD_ARGUMENTS) {
				throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
						"method request count errors,count:" + paraClazz.length);
      }
      // 检查第一项是否有controller
      if (!paraClazz[0].getName().equals(PROTOBUF_CONTROLLER)) {
        throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, "method controller errors:class:" + paraClazz[0].getName());
      }
      // 注册service接口的参数class
      // System.out.println(serviceClazz.getName());
      serviceRequestClassMap.putIfAbsent(getServiceKey(serviceClazz.getName(), method.getName()), paraClazz[1]);
      serviceResponseClassMap.putIfAbsent(getServiceKey(serviceClazz.getName(), method.getName()), method.getReturnType());
      // 注册service接口的参数type
      serviceRequestTypeMap.putIfAbsent(getServiceKey(serviceClazz.getName(), method.getName()), getTypeFromClass(paraClazz[1]));
      serviceResponseTypeMap.putIfAbsent(getServiceKey(serviceClazz.getName(), method.getName()), getTypeFromClass(method.getReturnType()));
    }
  }

  // 获取接口方法对应的request类型
  public Class<?> getRequestClass(String service, String method) {
    return serviceRequestClassMap.get(getServiceKey(service, method));
  }

  // 获取接口方法对应的response类型
  public Class<?> getResponseClass(String service, String method) {
    return serviceResponseClassMap.get(getServiceKey(service, method));
  }

  // 获取接口方法对应的request类型实例
  public com.google.protobuf.Message getRequestType(String service, String method) {
    return serviceRequestTypeMap.get(getServiceKey(service, method));
  }

  // 获取接口方法对应的response类型实例
  public com.google.protobuf.Message getResponseType(String service, String method) {
    return serviceResponseTypeMap.get(getServiceKey(service, method));
  }

  // 根据class类型获取对应类型实例
  private com.google.protobuf.Message getTypeFromClass(Class<?> clazz) {

    Method instanceMethod;
    try {
      instanceMethod = clazz.getMethod(DEFAULT_INSTANCE_METHOD);
    } catch (NoSuchMethodException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    com.google.protobuf.Message protoType;
    try {
      protoType = (Message) instanceMethod.invoke(null);
    } catch (IllegalAccessException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    } catch (InvocationTargetException e) {
      throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
    }

    return protoType;
  }

  // for test
  public int getRequestCount() {
    return serviceRequestClassMap.size();
  }

  // for test
  public int getResponseCount() {
    return serviceResponseClassMap.size();
  }
}
