/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufExporter.java
 * 创建人:xichen
 * 创建日期:2015年12月20日-下午10:11:40
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import java.util.Map;

import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.protocol.AbstractExporter;

/**
 * 类名称:ProtobufExporter 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月20日-下午10:11:40 修改备注:
 */
public class ProtobufExporter<T> extends AbstractExporter<T> {

  private final String key;

  private final Map<String, Exporter<?>> exporterMap;

  public ProtobufExporter(Invoker<T> invoker, String key, Map<String, Exporter<?>> exporterMap) {
    super(invoker);
    this.key = key;
    this.exporterMap = exporterMap;
  }

  @Override
  public void unexport() {
    super.unexport();
    exporterMap.remove(key);
  }

}
