/**
 * 包名:com.alibaba.dubbo.rpc.protocol.protobuf
 * 文件名:ProtobufController.java
 * 创建人:xichen
 * 创建日期:2015年12月23日-下午4:29:51
 * Copyright (c) 2015 Illuminate 公司-版权所有
 */
package com.alibaba.dubbo.rpc.protocol.protobuf;

import com.google.protobuf.RpcCallback;

/**
 * 类名称:ProtobufController 类描述: 创建人:xichen 修改人:xichen 修改时间:2015年12月23日-下午4:29:51 修改备注:
 */
public class ProtobufController implements com.google.protobuf.RpcController, java.io.Serializable {

  // 提供默认实例，避免创建
  private static final ProtobufController defaultInstance = new ProtobufController();

  public static ProtobufController getDefaultInstance() {
    return defaultInstance;
  }

  private String reason;
  private boolean failed;
  private boolean canceled;

  @SuppressWarnings("unused")
  private RpcCallback<Object> callback;

  public String errorText() {
    return reason;
  }

  public boolean failed() {
    return failed;
  }

  public boolean isCanceled() {
    return canceled;
  }

  public void notifyOnCancel(RpcCallback<Object> callback) {
    this.callback = callback;
  }

  public void reset() {
    reason = null;
    failed = false;
    canceled = false;
    callback = null;
  }

  public void setFailed(String reason) {
    this.reason = reason;
    this.failed = true;
  }

  public void startCancel() {
    canceled = true;
  }

}
